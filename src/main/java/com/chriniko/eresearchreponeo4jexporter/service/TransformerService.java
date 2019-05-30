package com.chriniko.eresearchreponeo4jexporter.service;

import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Author;
import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Entry;
import com.chriniko.eresearchreponeo4jexporter.repository.neo4j.AuthorRepository;
import com.chriniko.eresearchreponeo4jexporter.repository.neo4j.EntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TransformerService {

    private static final int BATCH_SIZE = 25;

    private final RecordService recordService;
    private final AuthorRepository authorRepository;
    private final EntryRepository entryRepository;
    private final SessionFactory sessionFactory;
    private final Partitioner partitioner;

    @Autowired
    public TransformerService(RecordService recordService,
                              AuthorRepository authorRepository,
                              EntryRepository entryRepository, SessionFactory sessionFactory,
                              Partitioner partitioner) {
        this.recordService = recordService;
        this.authorRepository = authorRepository;
        this.entryRepository = entryRepository;
        this.sessionFactory = sessionFactory;
        this.partitioner = partitioner;
    }


    public void etl() {
        Session session = sessionFactory.openSession();
        clearEntries(session);


        List<Entry> allEntries = recordService.findAllEntries();
        List<List<Entry>> splittedWork = partitioner.process(allEntries, BATCH_SIZE);

        log.info("total steps to execute: {}", splittedWork.size());

        int currentStep = 1;

        for (List<Entry> entries : splittedWork) {

            long startTime = System.nanoTime();
            log.info("executing step: {}", currentStep);

            Transaction tx = session.beginTransaction();
            try {
                process(entries);
                tx.commit();
            } catch (Exception error) {
                tx.rollback();
            } finally {
                tx.close();
            }

            long totalTime = System.nanoTime() - startTime;
            long totalTimeInMs = TimeUnit.MILLISECONDS.convert(totalTime, TimeUnit.NANOSECONDS);
            log.info("total time to execute step: {} is {} ms", currentStep, totalTimeInMs);
            currentStep++;
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

    private void process(List<Entry> entries) {
        for (Entry entry : entries) {

            String title = entry.getTitle().toLowerCase();

            com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry entryByTitleResult = entryRepository.findByTitleEquals(title);

            if (entryByTitleResult == null) {
                com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry entryToSave = new com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry();
                entryToSave.setId(UUID.randomUUID().toString());
                entryToSave.setTitle(entry.getTitle().toLowerCase());
                entryByTitleResult = entryRepository.save(entryToSave);
            }

            final List<com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author> authorsSaved = new ArrayList<>(entry.getAuthors().size());

            for (Author author : entry.getAuthors()) {

                String firstname = author.getFirstname().toLowerCase();
                String initials = author.getInitials().toLowerCase();
                String surname = author.getSurname().toLowerCase();

                com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author authorByNameResult
                        = authorRepository.findByFirstnameEqualsAndInitialsEqualsAndSurnameEquals(firstname, initials, surname);

                if (authorByNameResult == null) {

                    com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author authorToSave = new com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author();
                    authorToSave.setId(UUID.randomUUID().toString());
                    authorToSave.setFirstname(firstname);
                    authorToSave.setInitials(initials);
                    authorToSave.setSurname(surname);
                    authorByNameResult = authorRepository.save(authorToSave);
                }

                authorByNameResult.getParticipated().add(entryByTitleResult);
                authorByNameResult = authorRepository.save(authorByNameResult);

                authorsSaved.add(authorByNameResult);
            }

            entryByTitleResult.getAuthors().addAll(authorsSaved);
            entryRepository.save(entryByTitleResult);
        }
    }

}
