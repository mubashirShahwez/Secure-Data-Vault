package com.vault;

import com.vault.ui.LoginFrame;
import com.vault.core.DatabaseManager;
import javax.swing.*;
import java.io.FileInputStream;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
        loadConfiguration();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        if (!initializeDatabase()) {
            JOptionPane.showMessageDialog(null,
                    "Failed to connect to database.\nPlease check your configuration.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }

    private static void loadConfiguration() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("config.properties"));
            System.setProperty("db.type", props.getProperty("db.type", "mysql"));
            System.setProperty("db.url", props.getProperty("db.url"));
            System.setProperty("db.username", props.getProperty("db.username", ""));
            System.setProperty("db.password", props.getProperty("db.password", ""));
        } catch (Exception e) {
            System.setProperty("db.type", "mysql");
            System.setProperty("db.url", "jdbc:mysql://localhost:3306/vaultdb");
            System.setProperty("db.username", "root");
            System.setProperty("db.password", "password");
        }
    }

    private static boolean initializeDatabase() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if ("sqlite".equalsIgnoreCase(System.getProperty("db.type"))) {
                dbManager.createTablesIfNotExist();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
