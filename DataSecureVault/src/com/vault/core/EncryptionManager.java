package com.vault.core;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * EncryptionManager
 * - Derives AES-256 key from master password using PBKDF2WithHmacSHA256
 * - Encrypts/decrypts text (String) with AES/CBC/PKCS5Padding using random IV
 * - Encrypts/decrypts binary data (byte[]) for files using the same scheme
 * - Provides static password hashing utilities for user authentication
 */
public class EncryptionManager {

    private static final String SALT = "a9v5n38s";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionManager(String masterPassword) throws Exception {
        this.secretKey = deriveKey(masterPassword, SALT);
    }

    // ==================== Key Derivation ====================

    private SecretKey deriveKey(String password, String salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                ITERATIONS,
                KEY_LENGTH
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    // ==================== TEXT ENCRYPTION (String) ====================

    public String encrypt(String plain) throws Exception {
        if (plain == null) return null;

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decrypt(String base64Cipher) throws Exception {
        if (base64Cipher == null) return null;

        byte[] combined = Base64.getDecoder().decode(base64Cipher);

        byte[] iv = new byte[16];
        byte[] encrypted = new byte[combined.length - 16];
        System.arraycopy(combined, 0, iv, 0, 16);
        System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] plain = cipher.doFinal(encrypted);
        return new String(plain, StandardCharsets.UTF_8);
    }

    // ==================== FILE ENCRYPTION (byte[]) ====================

    /**
     * Encrypt raw file bytes. Returns IV || CIPHERTEXT (binary).
     */
    public byte[] encryptFile(byte[] fileData) throws Exception {
        if (fileData == null) return null;

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encrypted = cipher.doFinal(fileData);

        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return combined;
    }

    /**
     * Decrypt IV || CIPHERTEXT back to raw file bytes.
     */
    public byte[] decryptFile(byte[] encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.length < 17) return null;

        byte[] iv = new byte[16];
        byte[] encrypted = new byte[encryptedData.length - 16];
        System.arraycopy(encryptedData, 0, iv, 0, 16);
        System.arraycopy(encryptedData, 16, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        return cipher.doFinal(encrypted);
    }

    // ==================== PASSWORD HASHING (Static Utilities) ====================

    /**
     * Generate a random salt for password hashing (16 bytes, Base64-encoded).
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash a password using PBKDF2WithHmacSHA256.
     * Returns Base64-encoded hash (for storage in password_hash column).
     * Requires a salt parameter (generated via generateSalt()).
     */
    public static String hashPassword(String password, String saltBase64) throws Exception {
        byte[] salt = Base64.getDecoder().decode(saltBase64);

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 100000, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();

        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Overload for UserService that expects hashPassword(String).
     * Generates salt internally and returns "salt:hash".
     */
    public static String hashPassword(String password) throws Exception {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        return salt + ":" + hash;
    }

    /**
     * Verify a password against stored hash.
     * storedHash can be either:
     * - "salt:hash" format (new style), OR
     * - plain hash (old style, requires separate salt parameter)
     */
    public static boolean verifyPassword(String password, String storedHash, String salt) throws Exception {
        // Check if storedHash contains ":" (new format)
        if (storedHash.contains(":")) {
            String[] parts = storedHash.split(":", 2);
            String saltPart = parts[0];
            String hashPart = parts[1];
            String testHash = hashPassword(password, saltPart);
            return constantTimeEquals(testHash, hashPart);
        } else {
            // Old format: use provided salt parameter
            String testHash = hashPassword(password, salt);
            return constantTimeEquals(testHash, storedHash);
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}


