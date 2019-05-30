package com.chriniko.eresearchreponeo4jexporter.repository.neo4j;

import com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Entry;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntryRepository extends Neo4jRepository<Entry, String> {

    Entry findByTitleEquals(String title);
}
