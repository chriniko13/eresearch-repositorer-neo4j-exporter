package com.chriniko.eresearchreponeo4jexporter.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@ComponentScan("com.chriniko.eresearchreponeo4jexporter")
public class AppConfig {

    @Value("${service.zone.id}")
    private ZoneId zoneId;

    @Bean
    public Clock clock() {
        return Clock.system(zoneId);
    }
}
