package com.flypro.analytics.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configuration Jackson pour la sérialisation JSON.
 *
 * <p>Responsabilité future : garantir un format JSON homogène (dates ISO-8601,
 * enums, tolérance aux propriétés inconnues) pour tous les DTO de l'API.</p>
 */
@Configuration
public class JacksonConfig {

    /**
     * {@link ObjectMapper} principal de l'application.
     *
     * @param builder builder Spring Boot
     * @return ObjectMapper configuré
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                .modules(new JavaTimeModule())
                .featuresToDisable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .build();
    }
}
