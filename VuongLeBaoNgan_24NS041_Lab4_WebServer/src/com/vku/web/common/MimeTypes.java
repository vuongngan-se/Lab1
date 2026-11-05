package com.vku.web.common;

import java.util.HashMap;
import java.util.Map;

public class MimeTypes {
    private static final Map<String, String> MIME_MAP = new HashMap<>();

    static {
        MIME_MAP.put("html", "text/html; charset=UTF-8");
        MIME_MAP.put("htm", "text/html; charset=UTF-8");
        MIME_MAP.put("css", "text/css; charset=UTF-8");
        MIME_MAP.put("js", "application/javascript; charset=UTF-8");
        MIME_MAP.put("json", "application/json; charset=UTF-8");
        MIME_MAP.put("txt", "text/plain; charset=UTF-8");
        MIME_MAP.put("pdf", "application/pdf");
        MIME_MAP.put("mp3", "audio/mpeg");
        MIME_MAP.put("wav", "audio/wav");
        MIME_MAP.put("mp4", "video/mp4");
        MIME_MAP.put("webm", "video/webm");
        MIME_MAP.put("png", "image/png");
        MIME_MAP.put("jpg", "image/jpeg");
        MIME_MAP.put("jpeg", "image/jpeg");
        MIME_MAP.put("gif", "image/gif");
        MIME_MAP.put("svg", "image/svg+xml");
        MIME_MAP.put("zip", "application/zip");
    }

    public static String getContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        int dot = filename.lastIndexOf('.');
        if (dot != -1 && dot < filename.length() - 1) {
            String ext = filename.substring(dot + 1).toLowerCase();
            return MIME_MAP.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}
