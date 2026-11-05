package com.vku.web.common;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method = "GET";
    private String path = "/";
    private String version = "HTTP/1.1";
    private final Map<String, String> headers = new HashMap<>();
    private String body = "";
    private byte[] bodyBytes = new byte[0];

    public HttpRequest() {}

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Map<String, String> getHeaders() { return headers; }
    public void addHeader(String key, String val) { headers.put(key.toLowerCase(), val); }
    public String getHeader(String key) { return headers.get(key.toLowerCase()); }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public byte[] getBodyBytes() { return bodyBytes; }
    public void setBodyBytes(byte[] bodyBytes) { this.bodyBytes = bodyBytes; }
}
