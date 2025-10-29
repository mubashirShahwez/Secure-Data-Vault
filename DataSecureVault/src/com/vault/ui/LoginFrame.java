package com.vault.ui;

import com.vault.service.UserService;
import com.vault.model.User;
import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JCheckBox showPasswordCheckbox;
    private UserService userService;

    public LoginFrame() {
        super("Secure Data Vault - Login");

        try {
            userService = new UserService();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to initialize user service: " + e.getMessage(),
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        initializeComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 320);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void initializeComponents() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("🔐 Secure Data Vault", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Enter your credentials to continue", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        gbc.gridy = 1;
        mainPanel.add(subtitleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        mainPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        mainPanel.add(usernameField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel passwordLabel = new JLabel("Master Password:");
        passwordLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        mainPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        passwordField.setEchoChar('•');
        mainPanel.add(passwordField, gbc);

        gbc.gridy = 4;
        gbc.gridx = 1;
        showPasswordCheckbox = new JCheckBox("Show Password");
        showPasswordCheckbox.addActionListener(e -> {
            if (showPasswordCheckbox.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });
        mainPanel.add(showPasswordCheckbox, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(120, 35));
        loginButton.addActionListener(e -> handleLogin());
        buttonPanel.add(loginButton);

        registerButton = new JButton("Register");
        registerButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        registerButton.setPreferredSize(new Dimension(120, 35));
        registerButton.addActionListener(e -> handleRegister());
        buttonPanel.add(registerButton);

        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);

        passwordField.addActionListener(e -> handleLogin());

        setContentPane(mainPanel);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = userService.loginUser(username, password);

            if (user != null) {
                userService.logAccess(user.getId(), "LOGIN", null);

                JOptionPane.showMessageDialog(this,
                        "Welcome back, " + username + "!",
                        "Login Successful",
                        JOptionPane.INFORMATION_MESSAGE);

                dispose();
                SwingUtilities.invokeLater(() -> {
                    MainVaultFrame mainFrame = new MainVaultFrame(user, password);
                    mainFrame.setVisible(true);
                });
            } else {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password.\nPlease try again.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "An error occurred during login:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 6 characters long.\nPlease choose a stronger password.",
                    "Weak Password",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = userService.registerUser(username, password);

            if (user != null) {
                JOptionPane.showMessageDialog(this,
                        "Registration successful!\nYou can now log in with your credentials.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                passwordField.setText("");
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Registration Failed",
                    JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "An error occurred during registration:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
