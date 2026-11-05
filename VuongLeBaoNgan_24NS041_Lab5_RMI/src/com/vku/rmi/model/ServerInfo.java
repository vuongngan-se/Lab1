package com.vku.rmi.model;

import java.io.Serializable;

public class ServerInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String serverId;
    private String host;
    private int port;
    private int recordCount;
    private long uptime;
    private boolean partnerConnected;

    public ServerInfo() {}

    public ServerInfo(String serverId, String host, int port, int recordCount, long uptime, boolean partnerConnected) {
        this.serverId = serverId;
        this.host = host;
        this.port = port;
        this.recordCount = recordCount;
        this.uptime = uptime;
        this.partnerConnected = partnerConnected;
    }

    public String getServerId() { return serverId; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getRecordCount() { return recordCount; }
    public long getUptime() { return uptime; }
    public boolean isPartnerConnected() { return partnerConnected; }
}
