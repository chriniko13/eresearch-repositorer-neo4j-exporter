package com.chriniko.eresearchreponeo4jexporter.domain.mongo;

import lombok.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString(of = {"title", "authors"})
public class Entry implements Serializable {

    private String title;
    private Set<Author> authors;
    private Map<String, String> metadata;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return Objects.equals(title.toLowerCase(), entry.title.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(title.toLowerCase());
    }
}
