package com.chriniko.eresearchreponeo4jexporter.configuration;

import org.neo4j.ogm.config.Configuration.Builder;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableNeo4jRepositories(basePackages = {"com.chriniko.eresearchreponeo4jexporter.repository.neo4j"})
public class Neo4jConfig {

    //public static final String URL = "http://neo4j:movies@localhost:7474";

//    @Bean
//    public org.neo4j.ogm.config.Configuration getConfiguration() {
//        org.neo4j.ogm.config.Configuration config = new Builder().uri(URL).build();
//        return config;
//    }
//
//    @Bean
//    public SessionFactory getSessionFactory(org.neo4j.ogm.config.Configuration configuration) {
//        return new SessionFactory(configuration, "com.chriniko.eresearchreponeo4jexporter.domain.neo4j");
//    }
//
//    @Bean
//    public Neo4jTransactionManager transactionManager(SessionFactory sessionFactory) {
//        return new Neo4jTransactionManager(sessionFactory);
//    }

}
