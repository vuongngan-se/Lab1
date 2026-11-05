package com.vku.ftp.common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FtpLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private long timestamp;
    private String clientIp;
    private String username;
    private String action;      // LOGIN, LIST, RETR (DOWNLOAD), STOR (UPLOAD), DELE, MKD
    private String targetPath;
    private String status;      // SUCCESS, FAILED
    private long bytesTransferred;

    public FtpLog() {
        this.timestamp = System.currentTimeMillis();
        this.id = "LOG-" + this.timestamp + "-" + (int)(Math.random() * 1000);
    }

    public FtpLog(String clientIp, String username, String action, String targetPath, String status, long bytes) {
        this();
        this.clientIp = clientIp;
        this.username = username;
        this.action = action;
        this.targetPath = targetPath;
        this.status = status;
        this.bytesTransferred = bytes;
    }

    public String getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getFormattedTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }
    public String getClientIp() { return clientIp; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getTargetPath() { return targetPath; }
    public String getStatus() { return status; }
    public long getBytesTransferred() { return bytesTransferred; }
}
