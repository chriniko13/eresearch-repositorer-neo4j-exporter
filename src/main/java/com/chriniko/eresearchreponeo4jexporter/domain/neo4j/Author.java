package com.chriniko.eresearchreponeo4jexporter.domain.neo4j;

import lombok.*;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString

@EqualsAndHashCode(of = {"id"})

@NodeEntity
public class Author {

    @Id
    private String id;

    private String fullname;

    @Relationship(type = "PARTICIPATED", direction = "OUTGOING")
    private Set<Entry> participated = new LinkedHashSet<>();

}
