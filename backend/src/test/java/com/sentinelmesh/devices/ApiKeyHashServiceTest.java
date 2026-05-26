package com.sentinelmesh.devices;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApiKeyHashServiceTest {

    private final ApiKeyHashService apiKeyHashService = new ApiKeyHashService();

    @Test
    void hash_shouldReturnHashDifferentFromRawApiKey() {
        String rawApiKey = "sm_live_test_key_123";

        String hash = apiKeyHashService.hash(rawApiKey);

        assertNotNull(hash);
        assertNotEquals(rawApiKey, hash);
    }

    @Test
    void hash_shouldReturnConsistentHashForSameInput() {
        String rawApiKey = "sm_live_test_key_123";

        String hash1 = apiKeyHashService.hash(rawApiKey);
        String hash2 = apiKeyHashService.hash(rawApiKey);

        assertEquals(hash1, hash2);
    }

    @Test
    void hash_shouldLookLikeSha256HexString() {
        String rawApiKey = "sm_live_test_key_123";

        String hash = apiKeyHashService.hash(rawApiKey);

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void matches_shouldReturnTrueForCorrectApiKey() {
        String rawApiKey = "sm_live_test_key_123";
        String storedHash = apiKeyHashService.hash(rawApiKey);

        assertTrue(apiKeyHashService.matches(rawApiKey, storedHash));
    }

    @Test
    void matches_shouldReturnFalseForWrongApiKey() {
        String correctApiKey = "sm_live_correct_key";
        String wrongApiKey = "sm_live_wrong_key";

        String storedHash = apiKeyHashService.hash(correctApiKey);

        assertFalse(apiKeyHashService.matches(wrongApiKey, storedHash));
    }
}