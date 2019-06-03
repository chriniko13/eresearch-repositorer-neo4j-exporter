package com.chriniko.eresearchreponeo4jexporter.repository.neo4j;

import com.chriniko.eresearchreponeo4jexporter.domain.neo4j.Author;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository extends Neo4jRepository<Author, String> {

    Author findByFullnameEquals(String fullname);
}
