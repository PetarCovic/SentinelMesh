package com.sentinelmesh.simulator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonFieldExtractorTest {

    @Test
    void extractStringField_shouldExtractId() {
        String json = """
                {
                  "id": "device-123",
                  "apiKey": "secret-key"
                }
                """;

        assertEquals("device-123", JsonFieldExtractor.extractStringField(json, "id"));
    }

    @Test
    void extractStringField_shouldExtractApiKey() {
        String json = """
                {
                  "id": "device-123",
                  "apiKey": "secret-key"
                }
                """;

        assertEquals("secret-key", JsonFieldExtractor.extractStringField(json, "apiKey"));
    }

    @Test
    void extractStringField_shouldThrowWhenFieldMissing() {
        String json = """
                {
                  "id": "device-123"
                }
                """;

        assertThrows(IllegalStateException.class, () ->
                JsonFieldExtractor.extractStringField(json, "apiKey")
        );
    }
}