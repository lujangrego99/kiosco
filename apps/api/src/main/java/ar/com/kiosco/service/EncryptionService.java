package ar.com.kiosco.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data.
 * Uses AES-256-GCM for authenticated encryption.
 *
 * Data format: Base64(IV + ciphertext + authTag)
 * - IV: 12 bytes (96 bits)
 * - Ciphertext: variable length
 * - AuthTag: 16 bytes (128 bits, included by GCM mode)
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${encryption.key:}")
    private String encryptionKey;

    private SecretKey secretKey;
    private boolean encryptionEnabled = false;

    @PostConstruct
    public void init() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            log.warn("ENCRYPTION_KEY not configured - encryption disabled. " +
                    "Set ENCRYPTION_KEY environment variable to enable.");
            return;
        }

        try {
            // Derive a 256-bit key from the provided key using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            this.encryptionEnabled = true;
            log.info("Encryption service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize encryption service: {}", e.getMessage());
            throw new RuntimeException("Encryption initialization failed", e);
        }
    }

    /**
     * Encrypts a plaintext string.
     * @param plaintext The text to encrypt
     * @return Base64-encoded ciphertext (IV + encrypted data)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }

        if (!encryptionEnabled) {
            // Passthrough when encryption is disabled
            return plaintext;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64-encoded ciphertext.
     * @param ciphertext Base64-encoded encrypted data
     * @return Decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }

        if (!encryptionEnabled) {
            // Passthrough when encryption is disabled
            return ciphertext;
        }

        // Check if this looks like encrypted data (Base64 with typical length)
        if (!isEncryptedData(ciphertext)) {
            // This is likely unencrypted legacy data - return as-is
            return ciphertext;
        }

        try {
            // Decode Base64
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            // Extract IV and ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encryptedData = new byte[buffer.remaining()];
            buffer.get(encryptedData);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(encryptedData);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // If decryption fails, this might be unencrypted legacy data
            log.debug("Decryption failed for value, treating as plaintext: {}", e.getMessage());
            return ciphertext;
        }
    }

    /**
     * Generates a SHA-256 hash of the input for searching.
     * @param value The value to hash
     * @return Lowercase hex string of the hash
     */
    public String hash(String value) {
        if (value == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.toLowerCase().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Checks if encryption is enabled.
     */
    public boolean isEnabled() {
        return encryptionEnabled;
    }

    /**
     * Heuristic to detect if a string looks like encrypted data.
     * Encrypted data is Base64 and has a minimum length (IV + at least some ciphertext).
     */
    private boolean isEncryptedData(String data) {
        if (data == null || data.length() < 24) {
            // Minimum encrypted data: 12 bytes IV + 16 bytes auth tag = 28 bytes = ~38 Base64 chars
            return false;
        }

        // Check if it's valid Base64
        try {
            byte[] decoded = Base64.getDecoder().decode(data);
            // Must have at least IV length + auth tag length
            return decoded.length >= GCM_IV_LENGTH + 16;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
