package com.chriniko.eresearchreponeo4jexporter.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@ComponentScan("com.chriniko.eresearchreponeo4jexporter")
public class AppConfig {

    @Value("${service.zone.id}")
    private ZoneId zoneId;

    @Bean
    public Clock clock() {
        return Clock.system(zoneId);
    }

    @Bean
    @Qualifier("io-workers")
    public ThreadPoolExecutor ioWorkers() {

        int processors = Runtime.getRuntime().availableProcessors();

        return new ThreadPoolExecutor(
                processors,
                4 * processors,
                30,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                new ThreadFactory() {
                    private final AtomicLong workerIdx = new AtomicLong(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("io-worker-" + workerIdx.getAndIncrement());
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

    }

}
