package com.chriniko.eresearchreponeo4jexporter.configuration;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.util.Collection;
import java.util.Collections;

@Configuration
public class MongoConfig extends AbstractMongoConfiguration {

    @Bean
    public GridFSBucket getGridFSBuckets() {
        MongoDatabase db = mongoDbFactory().getDb();
        return GridFSBuckets.create(db);
    }

    @Override
    protected String getDatabaseName() {
        return "eresearch";
    }

    @Override
    protected Collection<String> getMappingBasePackages() {
        return Collections.singletonList("com.eresearch.repositorer.domain");
    }

    @Bean
    public GridFsTemplate gridFsTemplate() throws Exception {
        return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
    }

    @Override
    public MongoClient mongoClient() {
        return new MongoClient("localhost", 27017);
    }
}
