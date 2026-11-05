package com.vku.mail.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MailLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private long timestamp;
    private String protocol; // "SMTP" or "POP3"
    private String clientIp;
    private String details;
    private String status;

    public MailLog() {
        this.timestamp = System.currentTimeMillis();
        this.id = "LOG-" + this.timestamp + "-" + (int)(Math.random() * 1000);
    }

    public MailLog(String protocol, String clientIp, String details, String status) {
        this();
        this.protocol = protocol;
        this.clientIp = clientIp;
        this.details = details;
        this.status = status;
    }

    public String getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getFormattedDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }
    public String getProtocol() { return protocol; }
    public String getClientIp() { return clientIp; }
    public String getDetails() { return details; }
    public String getStatus() { return status; }
}
