package com.chriniko.eresearchreponeo4jexporter;

import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Entry;
import com.chriniko.eresearchreponeo4jexporter.service.RecordService;
import com.chriniko.eresearchreponeo4jexporter.service.TransformerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class EresearchRepoNeo4jExporterApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(EresearchRepoNeo4jExporterApplication.class, args);
    }


    @Autowired
    private RecordService recordService;

    @Autowired
    private TransformerService transformerService;

    @Override
    public void run(String... args) throws Exception {

        List<Entry> allEntries = recordService.findAllEntries();
        System.out.println(allEntries.size());

        transformerService.etl();

//        allEntries.forEach(entry -> {
//
//            String title = entry.getTitle();
//            System.out.println(title);
//
//        });

    }
}
