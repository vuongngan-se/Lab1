package com.vku.web.common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private String method;
    private int statusCode;
    private long timestamp;

    public HistoryItem(String url, String method, int statusCode) {
        this.url = url;
        this.method = method;
        this.statusCode = statusCode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getUrl() { return url; }
    public String getMethod() { return method; }
    public int getStatusCode() { return statusCode; }
    public long getTimestamp() { return timestamp; }

    public String getFormattedTime() {
        return new SimpleDateFormat("HH:mm:ss dd/MM").format(new Date(timestamp));
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] (" + method + " -> " + statusCode + ") " + url;
    }
}
