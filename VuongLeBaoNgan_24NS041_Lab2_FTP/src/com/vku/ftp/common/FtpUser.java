package com.vku.ftp.common;

import java.io.Serializable;

public class FtpUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String homeDirectory;
    private boolean canWrite;
    private boolean canDelete;
    private boolean enabled;

    public FtpUser() {}

    public FtpUser(String username, String password, String homeDirectory, boolean canWrite, boolean canDelete) {
        this.username = username;
        this.password = password;
        this.homeDirectory = homeDirectory;
        this.canWrite = canWrite;
        this.canDelete = canDelete;
        this.enabled = true;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getHomeDirectory() { return homeDirectory; }
    public void setHomeDirectory(String homeDirectory) { this.homeDirectory = homeDirectory; }

    public boolean isCanWrite() { return canWrite; }
    public void setCanWrite(boolean canWrite) { this.canWrite = canWrite; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
