package com.vku.chat.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String fullName;
    private String password;
    private boolean online;
    private long lastSeen;

    public User() {}

    public User(String username, String fullName, String password) {
        this.username = username;
        this.fullName = fullName;
        this.password = password;
        this.online = false;
        this.lastSeen = System.currentTimeMillis();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    @Override
    public String toString() {
        return fullName != null && !fullName.isEmpty() ? fullName + " (" + username + ")" : username;
    }
}
