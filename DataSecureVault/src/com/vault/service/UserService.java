package com.vault.service;

import com.vault.core.DatabaseManager;
import com.vault.core.EncryptionManager;
import com.vault.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserService {

    private final DatabaseManager db;

    public UserService() throws Exception {
        this.db = DatabaseManager.getInstance();
    }

    // ===================== Registration =====================

    public User registerUser(String username, String password) throws Exception {
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Choose ONE storage approach:
        // A) NEW STYLE: store "salt:hash" in password_hash; keep salt column NULL
        String saltAndHash = EncryptionManager.hashPassword(password);
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, NULL)";

        // B) OLD STYLE (uncomment to use): store hash in password_hash and salt separately
        // String salt = EncryptionManager.generateSalt();
        // String hash = EncryptionManager.hashPassword(password, salt);
        // String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";

        int userId;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            // NEW STYLE params
            ps.setString(1, username);
            ps.setString(2, saltAndHash);

            // OLD STYLE params (uncomment if you use OLD STYLE SQL above)
            // ps.setString(1, username);
            // ps.setString(2, hash);
            // ps.setString(3, salt);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new RuntimeException("Failed to create user");
                userId = rs.getInt(1);
            }
        }

        return new User(userId, username, /*passwordHash*/ saltAndHash, /*salt*/ null);
        // For OLD STYLE: return new User(userId, username, hash, salt);
    }

    // ===================== Login =====================

    public User loginUser(String username, String password) throws Exception {
        User user = findByUsername(username);
        if (user == null) return null;

        // Works for both formats:
        // NEW STYLE: password_hash = "salt:hash", salt = NULL
        // OLD STYLE: password_hash = hash only, salt = non-null
        boolean ok = EncryptionManager.verifyPassword(password, user.getPasswordHash(), user.getSalt());

        if (ok) {
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?")) {
                ps.setInt(1, user.getId());
                ps.executeUpdate();
            }
            return user;
        }
        return null;
    }

    // ===================== Logging =====================

    public void logAccess(int userId, String action, String keyName) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO access_logs (user_id, action, key_name) VALUES (?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, keyName);
            ps.executeUpdate();
        } catch (Exception e) {
            // Non-fatal: log and continue
            e.printStackTrace();
        }
    }

    // ===================== Helpers =====================

    public User findByUsername(String username) throws Exception {
        String sql = "SELECT id, username, password_hash, salt FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int id = rs.getInt("id");
                String uname = rs.getString("username");
                String passwordHash = rs.getString("password_hash");
                String salt = rs.getString("salt"); // may be NULL for new style
                return new User(id, uname, passwordHash, salt);
            }
        }
    }
}


