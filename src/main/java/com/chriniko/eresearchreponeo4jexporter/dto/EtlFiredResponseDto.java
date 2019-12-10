package com.chriniko.eresearchreponeo4jexporter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EtlFiredResponseDto {

    private String message;
    private long totalEntriesToTransform;

}
