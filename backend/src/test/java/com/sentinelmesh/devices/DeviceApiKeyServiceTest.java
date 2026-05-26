package com.sentinelmesh.devices;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DeviceApiKeyServiceTest {

    private final DeviceApiKeyService deviceApiKeyService = new DeviceApiKeyService();

    @Test
    void generateRawApiKey_shouldStartWithExpectedPrefix() {
        String apiKey = deviceApiKeyService.generateRawApiKey();

        assertNotNull(apiKey);
        assertTrue(apiKey.startsWith("sm_live_"));
    }

    @Test
    void generateRawApiKey_shouldGenerateDifferentKeysEachTime() {
        String key1 = deviceApiKeyService.generateRawApiKey();
        String key2 = deviceApiKeyService.generateRawApiKey();

        assertNotEquals(key1, key2);
    }

    @Test
    void generateRawApiKey_shouldBeLongEnoughForSecurity() {
        String apiKey = deviceApiKeyService.generateRawApiKey();

        assertTrue(apiKey.length() >= 40);
    }
}