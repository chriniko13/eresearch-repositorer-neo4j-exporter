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
public class Entry {

    @Id
    private String id;

    private String title;

    @Relationship(type = "CONTRIBUTED", direction = "OUTGOING")
    private Set<Author> authors = new LinkedHashSet<>();
}
