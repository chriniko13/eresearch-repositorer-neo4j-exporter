package com.chriniko.eresearchreponeo4jexporter.service;

import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Entry;
import com.chriniko.eresearchreponeo4jexporter.repository.mongo.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecordService {

    private final RecordRepository recordRepository;

    @Autowired
    public RecordService(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }


    public List<Entry> findAllEntries() {
        return recordRepository
                .findAll(true)
                .stream()
                .map(r -> r.getRecord().getEntries())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }



}
