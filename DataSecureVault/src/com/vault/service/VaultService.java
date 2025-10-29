package com.vault.service;

import com.vault.core.DatabaseManager;
import com.vault.core.EncryptionManager;
import com.vault.model.Secret;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VaultService {

    private final DatabaseManager db;
    private final EncryptionManager enc;

    // This matches MainVaultFrame: new VaultService(masterPassword)
    public VaultService(String masterPassword) throws Exception {
        this.db = DatabaseManager.getInstance();
        this.enc = new EncryptionManager(masterPassword);
    }

    // =============== CRUD for secrets ===============

    public List<Secret> getSecrets(int userId, String query) throws Exception {
        List<Secret> out = new ArrayList<>();
        String sql = "SELECT id, key_name, secret_value, created_at, updated_at " +
                "FROM vault_data WHERE user_id = ? " +
                (query == null || query.isEmpty() ? "" : "AND key_name LIKE ? ") +
                "ORDER BY updated_at DESC, created_at DESC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            if (query != null && !query.isEmpty()) {
                ps.setString(2, "%" + query + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Secret s = new Secret();
                    s.setId(rs.getInt("id"));
                    s.setKeyName(rs.getString("key_name"));
                    s.setEncryptedValue(rs.getString("secret_value"));
                    s.setCreatedAt(rs.getTimestamp("created_at"));
                    s.setUpdatedAt(rs.getTimestamp("updated_at"));
                    out.add(s);
                }
            }
        }
        return out;
    }

    public Secret getSecretById(int userId, int id) throws Exception {
        String sql = "SELECT id, key_name, secret_value, created_at, updated_at " +
                "FROM vault_data WHERE user_id = ? AND id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Secret s = new Secret();
                s.setId(rs.getInt("id"));
                s.setKeyName(rs.getString("key_name"));
                s.setEncryptedValue(rs.getString("secret_value"));
                s.setCreatedAt(rs.getTimestamp("created_at"));
                s.setUpdatedAt(rs.getTimestamp("updated_at"));
                // Decrypt on demand in UI: s.getDecryptedValue() uses EncryptionManager
                s.setDecryptedValue(enc.decrypt(s.getEncryptedValue()));
                return s;
            }
        }
    }

    public void addSecret(int userId, String keyName, String plainValue) throws Exception {
        String encVal = enc.encrypt(plainValue);
        String sql = "INSERT INTO vault_data (user_id, key_name, secret_value) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, keyName);
            ps.setString(3, encVal);
            ps.executeUpdate();
        }
    }

    public void updateSecret(int userId, int id, String keyName, String plainValue) throws Exception {
        String encVal = enc.encrypt(plainValue);
        String sql = "UPDATE vault_data SET key_name = ?, secret_value = ? WHERE user_id = ? AND id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, keyName);
            ps.setString(2, encVal);
            ps.setInt(3, userId);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    public void deleteSecret(int userId, int id) throws Exception {
        String sql = "DELETE FROM vault_data WHERE user_id = ? AND id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
