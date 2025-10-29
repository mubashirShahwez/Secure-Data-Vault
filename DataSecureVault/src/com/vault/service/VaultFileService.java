package com.vault.service;

import com.vault.core.DatabaseManager;
import com.vault.core.EncryptionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VaultFileService {

    public static class FileInfo {
        public final String fileName;
        public final int fileSize;
        public FileInfo(String fileName, int fileSize) {
            this.fileName = fileName; this.fileSize = fileSize;
        }
    }

    private final DatabaseManager dbManager;
    private final EncryptionManager encryption;

    public VaultFileService(String masterPassword) throws Exception {
        this.dbManager = DatabaseManager.getInstance();
        this.encryption = new EncryptionManager(masterPassword);
    }

    // Save file: encrypt bytes and insert into DB
    public void saveFile(int userId, String fileName, String fileType, byte[] rawBytes) throws Exception {
        if (rawBytes == null) throw new IllegalArgumentException("No file data");

        // 10 MB max (explicit service-side enforcement)
        if (rawBytes.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large. Max 10 MB.");
        }

        byte[] encrypted = encryption.encryptFile(rawBytes);

        String sql = "INSERT INTO vault_files (user_id, file_name, file_type, file_size, encrypted_data) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, fileName);
            ps.setString(3, fileType);
            ps.setInt(4, rawBytes.length);  // store original size
            ps.setBytes(5, encrypted);
            ps.executeUpdate();
        }
    }

    // List a user's files (name and original size)
    public List<FileInfo> getUserFiles(int userId) throws Exception {
        String sql = "SELECT file_name, file_size FROM vault_files WHERE user_id = ? ORDER BY id DESC";
        List<FileInfo> out = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new FileInfo(rs.getString("file_name"), rs.getInt("file_size")));
                }
            }
        }
        return out;
    }

    // Get decrypted bytes for preview/download
    public byte[] getFile(int userId, String fileName) throws Exception {
        String sql = "SELECT encrypted_data FROM vault_files WHERE user_id = ? AND file_name = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] encrypted = rs.getBytes("encrypted_data");
                    return encryption.decryptFile(encrypted);
                }
            }
        }
        return null;
    }

    // Return raw encrypted bytes (IV || CIPHERTEXT) for proof display
    public byte[] getEncryptedBytes(int userId, String fileName) throws Exception {
        String sql = "SELECT encrypted_data FROM vault_files WHERE user_id = ? AND file_name = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("encrypted_data");
                }
            }
        }
        return null;
    }
    private String headHex(byte[] data, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, data.length); i++) {
            sb.append(String.format("%02X ", data[i]));
        }
        return sb.toString().trim();
    }
}
