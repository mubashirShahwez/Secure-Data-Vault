package com.vault.model;

import java.sql.Timestamp;

public class AccessLog {
    private int id;
    private int userId;
    private String action;
    private String keyName;
    private Timestamp timestamp;
    private String ipAddress;

    public AccessLog() {}

    public AccessLog(int userId, String action, String keyName) {
        this.userId = userId;
        this.action = action;
        this.keyName = keyName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        return "AccessLog{action='" + action + "', keyName='" + keyName +
                "', timestamp=" + timestamp + "}";
    }
}
