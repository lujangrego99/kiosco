package ar.com.kiosco.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EncryptionService.
 * Verifies encryption, decryption, and hashing functionality.
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
    }

    @Nested
    @DisplayName("With Encryption Enabled")
    class WithEncryptionEnabled {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", "test-encryption-key-123");
            encryptionService.init();
        }

        @Test
        @DisplayName("should encrypt and decrypt string correctly")
        void shouldEncryptAndDecrypt() {
            String original = "test@example.com";

            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);

            assertNotEquals(original, encrypted);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("should produce different ciphertext for same plaintext (due to random IV)")
        void shouldProduceDifferentCiphertext() {
            String original = "test@example.com";

            String encrypted1 = encryptionService.encrypt(original);
            String encrypted2 = encryptionService.encrypt(original);

            assertNotEquals(encrypted1, encrypted2);
        }

        @Test
        @DisplayName("should handle null values")
        void shouldHandleNull() {
            assertNull(encryptionService.encrypt(null));
            assertNull(encryptionService.decrypt(null));
        }

        @Test
        @DisplayName("should handle empty string")
        void shouldHandleEmptyString() {
            String original = "";

            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("should encrypt special characters correctly")
        void shouldEncryptSpecialCharacters() {
            String original = "test@example.com!@#$%^&*()";

            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("should encrypt unicode characters correctly")
        void shouldEncryptUnicodeCharacters() {
            String original = "Usuario con ñ y áéíóú";

            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("should return original value for non-encrypted data")
        void shouldHandleLegacyData() {
            String plaintext = "not-encrypted@example.com";

            // Should return as-is since it's not in encrypted format
            String result = encryptionService.decrypt(plaintext);

            assertEquals(plaintext, result);
        }

        @Test
        @DisplayName("should be enabled")
        void shouldBeEnabled() {
            assertTrue(encryptionService.isEnabled());
        }
    }

    @Nested
    @DisplayName("With Encryption Disabled")
    class WithEncryptionDisabled {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", "");
            encryptionService.init();
        }

        @Test
        @DisplayName("should passthrough encryption")
        void shouldPassthroughEncryption() {
            String original = "test@example.com";

            String encrypted = encryptionService.encrypt(original);

            assertEquals(original, encrypted);
        }

        @Test
        @DisplayName("should passthrough decryption")
        void shouldPassthroughDecryption() {
            String original = "test@example.com";

            String decrypted = encryptionService.decrypt(original);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("should not be enabled")
        void shouldNotBeEnabled() {
            assertFalse(encryptionService.isEnabled());
        }
    }

    @Nested
    @DisplayName("Hash Function")
    class HashFunction {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", "test-key");
            encryptionService.init();
        }

        @Test
        @DisplayName("should produce consistent hash for same input")
        void shouldProduceConsistentHash() {
            String input = "test@example.com";

            String hash1 = encryptionService.hash(input);
            String hash2 = encryptionService.hash(input);

            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("should produce different hashes for different inputs")
        void shouldProduceDifferentHashes() {
            String input1 = "test@example.com";
            String input2 = "other@example.com";

            String hash1 = encryptionService.hash(input1);
            String hash2 = encryptionService.hash(input2);

            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("should handle null input")
        void shouldHandleNull() {
            assertNull(encryptionService.hash(null));
        }

        @Test
        @DisplayName("should normalize case for hashing")
        void shouldNormalizeCase() {
            String lower = "test@example.com";
            String upper = "TEST@EXAMPLE.COM";
            String mixed = "Test@Example.Com";

            String hashLower = encryptionService.hash(lower);
            String hashUpper = encryptionService.hash(upper);
            String hashMixed = encryptionService.hash(mixed);

            assertEquals(hashLower, hashUpper);
            assertEquals(hashLower, hashMixed);
        }

        @Test
        @DisplayName("should produce 64-character hex string")
        void shouldProduceHexString() {
            String input = "test@example.com";

            String hash = encryptionService.hash(input);

            assertEquals(64, hash.length());
            assertTrue(hash.matches("[0-9a-f]+"));
        }
    }
}
