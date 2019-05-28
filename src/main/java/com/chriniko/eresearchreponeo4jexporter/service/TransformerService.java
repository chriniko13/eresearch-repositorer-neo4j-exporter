package com.chriniko.eresearchreponeo4jexporter.service;

import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Author;
import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Entry;
import com.chriniko.eresearchreponeo4jexporter.repository.neo4j.AuthorRepository;
import com.chriniko.eresearchreponeo4jexporter.repository.neo4j.EntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TransformerService {

    private final RecordService recordService;

    private final AuthorRepository authorRepository;
    private final EntryRepository entryRepository;

    @Autowired
    public TransformerService(RecordService recordService,
                              AuthorRepository authorRepository,
                              EntryRepository entryRepository) {
        this.recordService = recordService;
        this.authorRepository = authorRepository;
        this.entryRepository = entryRepository;
    }

    public void etl() {

        authorRepository.deleteAll();
        entryRepository.deleteAll();


        List<Entry> allEntries = recordService.findAllEntries();

        for (Entry entry : allEntries) {

            //TODO check if entry exists, if not create

            com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry entryToSave = new com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry();
            entryToSave.setId(UUID.randomUUID().toString());
            entryToSave.setTitle(entry.getTitle());
            entryRepository.save(entryToSave);


            final List<com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author> authorsSaved = new ArrayList<>(entry.getAuthors().size());
            for (Author author : entry.getAuthors()) {

                //TODO check if author exists, if not create

                com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author authorToSave = new com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author();
                authorToSave.setId(UUID.randomUUID().toString());
                authorToSave.setFirstname(author.getFirstname());
                authorToSave.setInitials(author.getInitials());
                authorToSave.setSurname(author.getSurname());
                authorToSave.getParticipated().add(entryToSave);

                authorRepository.save(authorToSave);


                authorsSaved.add(authorToSave);
            }

            entryToSave.getAuthors().addAll(authorsSaved);
            entryRepository.save(entryToSave);


        }


    }

}
