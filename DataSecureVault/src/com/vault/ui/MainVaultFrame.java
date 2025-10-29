package com.vault.ui;

import com.vault.model.Secret;
import com.vault.model.User;
import com.vault.service.UserService;
import com.vault.service.VaultService;
import com.vault.service.VaultFileService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainVaultFrame extends JFrame {

    private final User currentUser;
    private final String masterPassword;

    private final VaultService vaultService;
    private final UserService userService;
    private final VaultFileService fileService;

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private JLabel statusLabel;

    public MainVaultFrame(User user, String masterPassword) {
        super("Secure Data Vault - " + user.getUsername());
        this.currentUser = user;
        this.masterPassword = masterPassword;

        try {
            this.vaultService = new VaultService(masterPassword);
            this.userService = new UserService();
            this.fileService = new VaultFileService(masterPassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize services: " + e.getMessage(), e);
        }

        initUI();
        loadSecrets("");

        setMinimumSize(new Dimension(960, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { handleLogout(); }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        JLabel title = new JLabel("🗝️ My Secure Vault");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.add(title, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchField = new JTextField(24);
        JButton searchBtn = new JButton("🔎");
        searchBtn.addActionListener(e -> loadSecrets(searchField.getText().trim()));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

// NEW: Ciphertext button in header (next to search)
        JButton viewCipherBtn = new JButton("🔑 View Cipher");     // NEW
        viewCipherBtn.addActionListener(e -> handleViewCipher());  // NEW
        searchPanel.add(viewCipherBtn);                            // NEW

        header.add(searchPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);


        // Table
        model = new DefaultTableModel(new Object[]{"ID", "Key Name", "Created", "Updated"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton addBtn = new JButton("➕ Add Secret");
        JButton viewBtn = new JButton("👁️ View Secret");
        JButton updateBtn = new JButton("✏️ Update");
        JButton deleteBtn = new JButton("🗑️ Delete");
        JButton refreshBtn = new JButton("🔄 Refresh");
        JButton logoutBtn = new JButton("🚪 Logout");

        JButton addFileBtn = new JButton("📎 Add File");
        JButton viewFilesBtn = new JButton("📁 View Files");

        // NEW: Preview and Cipher buttons for files
        JButton previewFileBtn = new JButton("🖼️ Preview File");            // NEW
        previewFileBtn.addActionListener(e -> handlePreviewFile());          // NEW
        JButton viewFileCipherBtn = new JButton("🔑 File Cipher");           // NEW
        viewFileCipherBtn.addActionListener(e -> handleViewFileCipher());    // NEW

        addBtn.addActionListener(e -> handleAdd());
        viewBtn.addActionListener(e -> handleView());
        updateBtn.addActionListener(e -> handleUpdate());
        deleteBtn.addActionListener(e -> handleDelete());
        refreshBtn.addActionListener(e -> loadSecrets(searchField.getText().trim()));
        logoutBtn.addActionListener(e -> handleLogout());

        addFileBtn.addActionListener(e -> handleAddFile());
        viewFilesBtn.addActionListener(e -> handleViewFiles());

        buttons.add(addBtn);
        buttons.add(viewBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(refreshBtn);
        buttons.add(logoutBtn);
        buttons.add(addFileBtn);
        buttons.add(viewFilesBtn);
        buttons.add(previewFileBtn);       // NEW
        buttons.add(viewFileCipherBtn);    // NEW

        statusLabel = new JLabel("Loaded 0 secret(s)");
        footer.add(buttons, BorderLayout.WEST);
        footer.add(statusLabel, BorderLayout.SOUTH);

        add(footer, BorderLayout.SOUTH);
    }

    private void loadSecrets(String query) {
        try {
            model.setRowCount(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            List<Secret> secrets = vaultService.getSecrets(currentUser.getId(), query);
            for (Secret s : secrets) {
                model.addRow(new Object[]{
                        s.getId(),
                        s.getKeyName(),
                        s.getCreatedAt() != null ? fmt.format(s.getCreatedAt().toLocalDateTime()) : "",
                        s.getUpdatedAt() != null ? fmt.format(s.getUpdatedAt().toLocalDateTime()) : ""
                });
            }
            statusLabel.setText("Loaded " + secrets.size() + " secret(s)");
        } catch (Exception ex) {
            showError("Failed to load secrets: " + ex.getMessage());
        }
    }

    private Integer getSelectedSecretId() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        Object val = model.getValueAt(row, 0);
        if (val == null) return null;
        return Integer.parseInt(val.toString());
    }

    // ================== Actions ==================

    private void handleAdd() {
        AddSecretDialog dialog = new AddSecretDialog(
                this,
                currentUser,
                vaultService,
                userService
        );
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            try {
                loadSecrets(searchField.getText().trim());
            } catch (Exception ex) {
                showError("Failed to refresh list: " + ex.getMessage());
            }
        }
    }

    private void handleView() {
        Integer id = getSelectedSecretId();
        if (id == null) {
            showWarn("Please select a secret to view.");
            return;
        }
        try {
            Secret s = vaultService.getSecretById(currentUser.getId(), id);
            if (s == null) {
                showWarn("Secret not found.");
                return;
            }
            userService.logAccess(currentUser.getId(), "VIEW", s.getKeyName());
            new ViewSecretDialog(this, s.getKeyName(), s.getDecryptedValue()).setVisible(true);
        } catch (Exception ex) {
            showError("Failed to decrypt/view secret: " + ex.getMessage());
        }
    }
    // NEW: View raw ciphertext stored in DB
    private void handleViewCipher() {
        Integer id = getSelectedSecretId();
        if (id == null) { showWarn("Please select a secret first."); return; }
        try {
            Secret s = vaultService.getSecretById(currentUser.getId(), id);
            if (s == null) { showWarn("Secret not found."); return; }
            new ViewCipherDialog(this, s.getKeyName(), s.getEncryptedValue()).setVisible(true);
        } catch (Exception ex) {
            showError("Failed to load ciphertext: " + ex.getMessage());
        }
    }
    private void handleUpdate() {
        Integer id = getSelectedSecretId();
        if (id == null) {
            showWarn("Please select a secret to update.");
            return;
        }
        try {
            Secret s = vaultService.getSecretById(currentUser.getId(), id);
            if (s == null) {
                showWarn("Secret not found.");
                return;
            }
            UpdateSecretDialog dialog = new UpdateSecretDialog(
                    this,
                    currentUser,
                    s.getId(),
                    s.getKeyName(),
                    s.getDecryptedValue(),
                    vaultService,
                    userService
            );
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadSecrets(searchField.getText().trim());
            }
        } catch (Exception ex) {
            showError("Failed to update secret: " + ex.getMessage());
        }
    }

    private void handleDelete() {
        Integer id = getSelectedSecretId();
        if (id == null) {
            showWarn("Please select a secret to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete selected secret?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            vaultService.deleteSecret(currentUser.getId(), id);
            userService.logAccess(currentUser.getId(), "DELETE", String.valueOf(id));
            loadSecrets(searchField.getText().trim());
        } catch (Exception ex) {
            showError("Failed to delete: " + ex.getMessage());
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Logout and close the vault?", "Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    // ================== File Features ==================

    private void handleAddFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Common Files (*.txt, *.pdf, *.jpg, *.png, *.docx, *.xlsx, *.zip)",
                "txt", "pdf", "jpg", "jpeg", "png", "docx", "xlsx", "zip"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (file.length() > 10L * 1024 * 1024) {
                showError("File too large. Max allowed is 10 MB.");
                return;
            }

            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                String fileType = getFileExtension(file.getName());
                fileService.saveFile(currentUser.getId(), file.getName(), fileType, fileData);
                userService.logAccess(currentUser.getId(), "ADD_FILE", file.getName());
                showInfo("File encrypted and saved: " + file.getName());
            } catch (Exception ex) {
                showError("Failed to save file: " + ex.getMessage());
            }
        }
    }

    private void handleViewFiles() {
        try {
            List<VaultFileService.FileInfo> files = fileService.getUserFiles(currentUser.getId());
            if (files.isEmpty()) {
                showInfo("No files stored yet.");
                return;
            }

            String[] options = files.stream()
                    .map(f -> f.fileName + " (" + formatSize(f.fileSize) + ")")
                    .toArray(String[]::new);

            String choice = (String) JOptionPane.showInputDialog(
                    this,
                    "Select a file to download:",
                    "Stored Files",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == null) return;
            String fileName = choice.split(" \\(")[0];
            downloadFile(fileName);
        } catch (Exception ex) {
            showError("Failed to list files: " + ex.getMessage());
        }
    }

    // Preview images and text files in-app
    // Preview images and text files in-app
    private void handlePreviewFile() {
        try {
            var files = fileService.getUserFiles(currentUser.getId());
            if (files.isEmpty()) { showInfo("No files stored yet."); return; }

            String[] names = files.stream()
                    .map(f -> f.fileName + " (" + formatSize(f.fileSize) + ")")
                    .toArray(String[]::new);

            String choice = (String) JOptionPane.showInputDialog(
                    this, "Select a file to preview:", "Preview",
                    JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
            if (choice == null) return;

            String fileName = choice.split(" \\(")[0];
            byte[] data = fileService.getFile(currentUser.getId(), fileName);
            if (data == null) { showError("File not found."); return; }

            String ext = getFileExtension(fileName);
            switch (ext) {
                case "png":
                case "jpg":
                case "jpeg":
                case "gif":
                    showImagePreview(fileName, data);
                    break;
                case "txt":
                case "csv":
                case "log":
                    showTextPreview(fileName, data);
                    break;
                default:
                    int c = JOptionPane.showConfirmDialog(this,
                            "Preview not supported for ." + ext + ". Save and open externally?",
                            "Preview", JOptionPane.YES_NO_OPTION);
                    if (c == JOptionPane.YES_OPTION) {
                        downloadFile(fileName);
                    }
            }
        } catch (Exception ex) {
            showError("Failed to preview file: " + ex.getMessage());
        }
    }
    // NEW: View raw encrypted bytes (Base64) of a stored file
    private void handleViewFileCipher() {                                // NEW
        try {                                                            // NEW
            var files = fileService.getUserFiles(currentUser.getId());   // NEW
            if (files.isEmpty()) { showInfo("No files stored yet."); return; } // NEW

            String[] names = files.stream()                               // NEW
                    .map(f -> f.fileName + " (" + formatSize(f.fileSize) + ")") // NEW
                    .toArray(String[]::new);                              // NEW

            String choice = (String) JOptionPane.showInputDialog(         // NEW
                    this, "Select a file:", "File Cipher",                // NEW
                    JOptionPane.PLAIN_MESSAGE, null, names, names[0]);    // NEW
            if (choice == null) return;                                   // NEW

            String fileName = choice.split(" \\(")[0];                    // NEW
            byte[] encrypted = fileService.getEncryptedBytes(currentUser.getId(), fileName); // NEW
            if (encrypted == null) { showError("Encrypted data not found."); return; }       // NEW

            String b64 = java.util.Base64.getEncoder().encodeToString(encrypted); // NEW
            JTextArea area = new JTextArea(14, 60);                      // NEW
            area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true); // NEW
            area.setText(b64);                                           // NEW
            JScrollPane scroll = new JScrollPane(area);                  // NEW
            JDialog dlg = new JDialog(this, "Ciphertext: " + fileName, true); // NEW
            dlg.getContentPane().add(scroll);                            // NEW
            dlg.setSize(760, 520);                                       // NEW
            dlg.setLocationRelativeTo(this);                             // NEW
            dlg.setVisible(true);                                        // NEW
        } catch (Exception ex) {                                         // NEW
            showError("Failed to load file ciphertext: " + ex.getMessage()); // NEW
        }                                                                // NEW
    }
    private void showImagePreview(String fileName, byte[] data) {
        ImageIcon icon = new ImageIcon(data);
        Image img = icon.getImage();
        // scale down if too large
        int maxW = 640, maxH = 480;
        int w = icon.getIconWidth(), h = icon.getIconHeight();
        if (w > maxW || h > maxH) {
            double scale = Math.min((double)maxW / w, (double)maxH / h);
            img = img.getScaledInstance((int)(w*scale), (int)(h*scale), Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
        }
        JLabel label = new JLabel(icon);
        JScrollPane scroll = new JScrollPane(label);
        JDialog dlg = new JDialog(this, "Preview: " + fileName, true);
        dlg.getContentPane().add(scroll);
        dlg.setSize(700, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void showTextPreview(String fileName, byte[] data) {
        String text;
        try { text = new String(data, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { text = "[Unable to decode text]"; }

        JTextArea area = new JTextArea(text, 20, 60);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(area);

        JDialog dlg = new JDialog(this, "Preview: " + fileName, true);
        dlg.getContentPane().add(scroll);
        dlg.setSize(700, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void downloadFile(String fileName) {
        try {
            byte[] data = fileService.getFile(currentUser.getId(), fileName);
            if (data == null) {
                showError("File not found.");
                return;
            }

            JFileChooser saver = new JFileChooser();
            saver.setSelectedFile(new File(fileName));
            if (saver.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                Files.write(saver.getSelectedFile().toPath(), data);
                userService.logAccess(currentUser.getId(), "DOWNLOAD_FILE", fileName);
                showInfo("File decrypted and saved: " + saver.getSelectedFile().getAbsolutePath());
            }
        } catch (Exception ex) {
            showError("Failed to download file: " + ex.getMessage());
        }
    }

    // ================== Utils ==================

    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    private void showWarn(String msg) { JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void showInfo(String msg) { JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE); }

    private String getFileExtension(String name) {
        int i = name.lastIndexOf('.');
        return (i > 0 && i < name.length() - 1) ? name.substring(i + 1).toLowerCase() : "unknown";
    }

    private String formatSize(int bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}
