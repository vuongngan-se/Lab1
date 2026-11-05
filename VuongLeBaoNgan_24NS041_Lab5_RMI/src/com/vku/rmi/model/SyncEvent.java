package com.vku.rmi.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SyncEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { CREATE, UPDATE, DELETE, FULL_SYNC, HEARTBEAT }

    private String id;
    private long timestamp;
    private Type type;
    private String recordId;
    private String originServer;
    private String targetServer;
    private String description;

    public SyncEvent(Type type, String recordId, String originServer, String targetServer, String description) {
        this.timestamp = System.currentTimeMillis();
        this.id = "EVT-" + this.timestamp;
        this.type = type;
        this.recordId = recordId;
        this.originServer = originServer;
        this.targetServer = targetServer;
        this.description = description;
    }

    public String getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public Type getType() { return type; }
    public String getRecordId() { return recordId; }
    public String getOriginServer() { return originServer; }
    public String getTargetServer() { return targetServer; }
    public String getDescription() { return description; }

    public String getFormattedTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (%s): %s",
                getFormattedTime(), originServer, targetServer, type, description);
    }
}
