package com.vault.model;

import java.sql.Timestamp;

public class Secret {

    private int id;
    private String keyName;
    private String encryptedValue;
    // transient holder for decrypted value used by UI (not stored in DB)
    private String decryptedValue;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    // ===== Getters / Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }

    public String getEncryptedValue() { return encryptedValue; }
    public void setEncryptedValue(String encryptedValue) { this.encryptedValue = encryptedValue; }

    public String getDecryptedValue() { return decryptedValue; }
    public void setDecryptedValue(String decryptedValue) { this.decryptedValue = decryptedValue; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
