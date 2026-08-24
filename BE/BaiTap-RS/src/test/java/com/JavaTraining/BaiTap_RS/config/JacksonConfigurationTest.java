package com.JavaTraining.BaiTap_RS.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JacksonConfigurationTest {

        @Test
        void objectMapperSerializesLocalDateCorrectly() throws JsonProcessingException {
                ObjectMapper mapper = new JacksonConfiguration().objectMapper();
                String json = mapper.writeValueAsString(Map.of("dateOfBirth", LocalDate.of(1990, 5, 20)));
                Assertions.assertTrue(
                                json.contains("\"dateOfBirth\":\"1990-05-20\""),
                                "LocalDate should be serialized to ISO-8601 string");
        }

        @Test
        void objectMapperSerializesLocalDateTimeCorrectly() throws JsonProcessingException {
                ObjectMapper mapper = new JacksonConfiguration().objectMapper();
                String json = mapper.writeValueAsString(Map.of("createdAt", LocalDateTime.of(2026, 8, 24, 9, 30, 0)));
                Assertions.assertTrue(
                                json.contains("\"createdAt\":\"2026-08-24T09:30:00\""),
                                "LocalDateTime should be serialized to ISO-8601 string");
        }
}
