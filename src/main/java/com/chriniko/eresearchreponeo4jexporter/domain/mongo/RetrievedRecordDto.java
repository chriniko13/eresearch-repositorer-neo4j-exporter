package com.chriniko.eresearchreponeo4jexporter.domain.mongo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(of = {"filename"})
@EqualsAndHashCode(of = {"filename"})

@JsonInclude(value = JsonInclude.Include.NON_NULL) //in order to not include null records.
public class RetrievedRecordDto {

    private String filename;
    private Record record;
}
