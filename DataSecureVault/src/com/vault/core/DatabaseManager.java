package com.vault.core;

import java.sql.*;

public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;
    private String dbType;
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;

    private DatabaseManager() throws SQLException {
        loadConfiguration();
        connect();
    }

    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void loadConfiguration() {
        this.dbType = System.getProperty("db.type", "mysql");
        this.dbUrl = System.getProperty("db.url");
        this.dbUsername = System.getProperty("db.username", "");
        this.dbPassword = System.getProperty("db.password", "");

        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new RuntimeException("Database URL not configured");
        }
    }

    private void connect() throws SQLException {
        try {
            if ("sqlite".equalsIgnoreCase(dbType)) {
                Class.forName("org.sqlite.JDBC");
            } else if ("mysql".equalsIgnoreCase(dbType)) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }

            if (dbUsername.isEmpty()) {
                connection = DriverManager.getConnection(dbUrl);
            } else {
                connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            }

            System.out.println("Database connected: " + dbType);

        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver not found: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    public void createTablesIfNotExist() throws SQLException {
        if (!"sqlite".equalsIgnoreCase(dbType)) {
            return;
        }

        Connection conn = getConnection();

        String createUsersTable =
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT NOT NULL UNIQUE, " +
                        "password_hash TEXT NOT NULL, " +
                        "salt TEXT NOT NULL, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "last_login TIMESTAMP)";

        String createVaultDataTable =
                "CREATE TABLE IF NOT EXISTS vault_data (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "key_name TEXT NOT NULL, " +
                        "encrypted_value TEXT NOT NULL, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                        "UNIQUE(user_id, key_name))";

        String createAccessLogsTable =
                "CREATE TABLE IF NOT EXISTS access_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "action TEXT NOT NULL, " +
                        "key_name TEXT, " +
                        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "ip_address TEXT, " +
                        "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createVaultDataTable);
            stmt.execute(createAccessLogsTable);
            System.out.println("SQLite tables created/verified");
        }
    }

    public ResultSet executeQuery(String sql, Object... params) throws SQLException {
        PreparedStatement pstmt = prepareStatement(sql, params);
        return pstmt.executeQuery();
    }

    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (PreparedStatement pstmt = prepareStatement(sql, params)) {
            return pstmt.executeUpdate();
        }
    }

    private PreparedStatement prepareStatement(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);

        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }

        return pstmt;
    }

    public int executeInsertAndGetId(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }

    public String getDbType() {
        return dbType;
    }
}
