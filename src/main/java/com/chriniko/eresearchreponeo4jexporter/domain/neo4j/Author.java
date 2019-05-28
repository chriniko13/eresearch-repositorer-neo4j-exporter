package com.chriniko.eresearchreponeo4jexporter.domain.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString

@NodeEntity
public class Author {

    @Id
    private String id;

    private String firstname;
    private String initials;
    private String surname;

    @Relationship(type = "PARTICIPATED", direction = "OUTGOING")
    private List<Entry> participated = new ArrayList<>();

}
