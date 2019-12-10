package com.chriniko.eresearchreponeo4jexporter.resource;


import com.chriniko.eresearchreponeo4jexporter.dto.EtlFiredResponseDto;
import com.chriniko.eresearchreponeo4jexporter.service.RecordService;
import com.chriniko.eresearchreponeo4jexporter.service.TransformerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("neo4j-exporter/api")
public class TransformerResource {

    private final TransformerService transformerService;
    private final RecordService recordService;
    private final ThreadPoolExecutor ioWorkers;

    @Autowired
    public TransformerResource(TransformerService transformerService,
                               RecordService recordService,
                               @Qualifier("io-workers") ThreadPoolExecutor ioWorkers) {
        this.transformerService = transformerService;
        this.recordService = recordService;
        this.ioWorkers = ioWorkers;
    }

    @PostMapping(path = "etl")
    public DeferredResult<HttpEntity<EtlFiredResponseDto>> performEtl() {

        DeferredResult<HttpEntity<EtlFiredResponseDto>> df = new DeferredResult<>(
                1000L,
                new EtlFiredResponseDto("could not fire etl, error occurred!", 0)
        );

        ioWorkers.submit(() -> {
            df.setResult(ResponseEntity.ok(
                    new EtlFiredResponseDto("etl fired successfully", recordService.findAllEntries().size()))
            );
            transformerService.etl();
        });


        return df;
    }

}
