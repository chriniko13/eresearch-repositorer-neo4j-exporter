package com.chriniko.eresearchreponeo4jexporter.service;

import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Author;
import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Entry;
import com.chriniko.eresearchreponeo4jexporter.error.ProcessingException;
import com.chriniko.eresearchreponeo4jexporter.repository.neo4j.AuthorRepository;
import com.chriniko.eresearchreponeo4jexporter.repository.neo4j.EntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
@Service
public class TransformerService {

    private static final boolean SINGLE_THREAD = false;

    private static final int BATCH_SIZE = 25;

    private final RecordService recordService;
    private final AuthorRepository authorRepository;
    private final EntryRepository entryRepository;
    private final SessionFactory sessionFactory;
    private final Partitioner partitioner;
    private final Set<String> titlesToSkip;
    private final ThreadPoolExecutor ioWorkers;

    @Autowired
    public TransformerService(RecordService recordService,
                              AuthorRepository authorRepository,
                              EntryRepository entryRepository, SessionFactory sessionFactory,
                              Partitioner partitioner,
                              @Qualifier("io-workers") ThreadPoolExecutor ioWorkers) {
        this.recordService = recordService;
        this.authorRepository = authorRepository;
        this.entryRepository = entryRepository;
        this.sessionFactory = sessionFactory;
        this.partitioner = partitioner;

        this.titlesToSkip = new LinkedHashSet<>();
        this.titlesToSkip.add("home page"); // Note: not necessary info from DBLP.
        this.ioWorkers = ioWorkers;
    }

    public void etl() {
        Session session = sessionFactory.openSession();
        clearEntries(session);


        List<Entry> allEntries = recordService.findAllEntries();
        log.info("allEntries size : " + allEntries.size());

        List<List<Entry>> splittedWork = partitioner.process(allEntries, BATCH_SIZE);
        int totalRecords = splittedWork.stream().map(List::size).reduce(0, Integer::sum);
        log.info("splittedWork size : " + splittedWork.size() + ", totalRecords: " + totalRecords);


        log.info("total steps to execute: {}", splittedWork.size());

        if (SINGLE_THREAD) {
            log.info("single thread approach");

            int currentStep = 1;

            for (List<Entry> entries : splittedWork) {

                long startTime = System.nanoTime();
                log.info("executing step: {}", currentStep);

                txTemplate(session, this::process, entries);

                long totalTime = System.nanoTime() - startTime;
                long totalTimeInMs = TimeUnit.MILLISECONDS.convert(totalTime, TimeUnit.NANOSECONDS);
                log.info("total time to execute step: {} is {} ms", currentStep, totalTimeInMs);
                currentStep++;
            }

        } else {
            log.info("multi thread approach");

            final ConcurrentHashMap<String, Semaphore> semaphoresByEntryTitle = new ConcurrentHashMap<>();
            final ConcurrentHashMap<String, Semaphore> semaphoresByAuthorName = new ConcurrentHashMap<>();


            final CountDownLatch countDownLatch = new CountDownLatch(splittedWork.size());

            final AtomicInteger currentStep = new AtomicInteger(1);

            for (List<Entry> entries : splittedWork) {

                CompletableFuture.runAsync(() -> {

                    long startTime = System.nanoTime();
                    log.info("executing step: {}, entries size: {}", currentStep.getAndIncrement(), entries.size());

                    txProcess(session, entries, semaphoresByEntryTitle, semaphoresByAuthorName);

                    long totalTime = System.nanoTime() - startTime;
                    long totalTimeInMs = TimeUnit.MILLISECONDS.convert(totalTime, TimeUnit.NANOSECONDS);
                    log.info("total time to execute step: {} is {} ms", currentStep, totalTimeInMs);

                    countDownLatch.countDown();

                }, ioWorkers);

            }

            try {
                log.info("waiting for workers to finish...");
                countDownLatch.await();
                log.info("all workers have finished...");
            } catch (InterruptedException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new ProcessingException(e);
                }
            }

        }

    }

    private void clearEntries(Session session) {
        Transaction transaction = session.beginTransaction();
        try {
            authorRepository.deleteAll();
            entryRepository.deleteAll();
            transaction.commit();
        } catch (Exception error) {
            transaction.rollback();
        } finally {
            transaction.close();
        }
    }

    private <R> void txTemplate(Session session, Consumer<List<R>> recordsConsumer, List<R> records) {

        Transaction tx = session.beginTransaction(Transaction.Type.READ_WRITE);
        try {
            recordsConsumer.accept(records);
            tx.commit();
        } catch (Exception error) {
            tx.rollback();
        } finally {
            tx.close();
        }
    }

    private void txProcess(Session session, List<Entry> records,
                           ConcurrentHashMap<String, Semaphore> semaphoresByEntryTitle,
                           ConcurrentHashMap<String, Semaphore> semaphoresByAuthorName) {

        Transaction tx = session.beginTransaction(Transaction.Type.READ_WRITE);
        try {
            process(records, semaphoresByEntryTitle, semaphoresByAuthorName);
            tx.commit();
        } catch (Exception error) {
            tx.rollback();
        } finally {
            tx.close();
        }
    }


    private void process(List<Entry> entries,
                         ConcurrentHashMap<String, Semaphore> semaphoresByEntryTitle,
                         ConcurrentHashMap<String, Semaphore> semaphoresByAuthorName) {
        entries.forEach(entry -> process(entry, semaphoresByEntryTitle, semaphoresByAuthorName));
    }

    private void process(Entry entry,
                         ConcurrentHashMap<String, Semaphore> semaphoresByEntryTitle,
                         ConcurrentHashMap<String, Semaphore> semaphoresByAuthorName) {

        String title = entry.getTitle().toLowerCase();
        if (titlesToSkip.contains(title)) {
            return;
        }


        String titleToGetSemaphore = title.trim().toLowerCase();
        Semaphore titleSemaphore = semaphoresByEntryTitle.compute(titleToGetSemaphore, (k, v) -> {
            if (v == null) {
                v = new Semaphore(1);
            }
            return v;
        });

        // Note: create entry if not exists.
        com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry entryByTitleResult = null;
        try {
            titleSemaphore.acquire();
            entryByTitleResult = createEntryIfNotExists(entry, title);
        } catch (InterruptedException e) {
            if (!Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            throw new CancellationException("interrupted");
        } finally {
            titleSemaphore.release();
        }


        for (Author author : entry.getAuthors()) {

            String fullName = Author.fullname(author);

            String authorFullnameToGetSemaphore = fullName.trim().toLowerCase();
            Semaphore authorSemaphore = semaphoresByAuthorName.compute(authorFullnameToGetSemaphore, (k, v) -> {
                if (v == null) {
                    v = new Semaphore(1);
                }
                return v;
            });


            // Note: create author if not exists.
            com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author authorByNameResult = null;
            try {
                authorSemaphore.acquire();
                authorByNameResult = createAuthorIfNotExists(fullName);
            } catch (InterruptedException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                }
                throw new CancellationException("interrupted");
            } finally {
                authorSemaphore.release();
            }


            if (!authorByNameResult.getParticipated().contains(entryByTitleResult)) {
                authorByNameResult.getParticipated().add(entryByTitleResult);
                authorByNameResult = authorRepository.save(authorByNameResult);
                entryByTitleResult.getAuthors().add(authorByNameResult);
            }

        }

        entryRepository.save(entryByTitleResult);
    }

    private void process(List<Entry> entries) {
        entries.forEach(this::process);
    }

    private void process(Entry entry) {
        String title = entry.getTitle().toLowerCase();
        if (titlesToSkip.contains(title)) {
            return;
        }

        com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry entryByTitleResult = createEntryIfNotExists(entry, title);


        for (Author author : entry.getAuthors()) {

            String fullName = Author.fullname(author);

            com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author authorByNameResult = createAuthorIfNotExists(fullName);


            if (!authorByNameResult.getParticipated().contains(entryByTitleResult)) {
                authorByNameResult.getParticipated().add(entryByTitleResult);
                authorByNameResult = authorRepository.save(authorByNameResult);
                entryByTitleResult.getAuthors().add(authorByNameResult);
            }

        }

        entryRepository.save(entryByTitleResult);
    }

    private com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry createEntryIfNotExists(Entry entry, String title) {
        com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry entryByTitleResult = entryRepository.findByTitleEquals(title);
        if (entryByTitleResult == null) {
            com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry entryToSave = new com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry();
            entryToSave.setId(UUID.randomUUID().toString());
            entryToSave.setTitle(entry.getTitle().toLowerCase());
            entryByTitleResult = entryRepository.save(entryToSave);
        }
        return entryByTitleResult;
    }

    private com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author createAuthorIfNotExists(String fullName) {
        com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author authorByNameResult = authorRepository.findByFullnameEquals(fullName);
        if (authorByNameResult == null) {
            com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author authorToSave = new com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author();
            authorToSave.setId(UUID.randomUUID().toString());
            authorToSave.setFullname(fullName);
            authorByNameResult = authorRepository.save(authorToSave);
        }
        return authorByNameResult;
    }


}
