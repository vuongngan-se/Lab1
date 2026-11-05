package com.vku.web.common;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private int statusCode = 200;
    private String statusMessage = "OK";
    private final Map<String, String> headers = new HashMap<>();
    private byte[] bodyBytes = new byte[0];

    public HttpResponse() {}

    public HttpResponse(int code, String msg) {
        this.statusCode = code;
        this.statusMessage = msg;
    }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeader(String key, String val) { headers.put(key, val); }
    public String getHeader(String key) { return headers.get(key); }

    public byte[] getBodyBytes() { return bodyBytes; }
    public void setBodyBytes(byte[] bodyBytes) { this.bodyBytes = bodyBytes; }

    public String getBodyAsString() {
        return new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
