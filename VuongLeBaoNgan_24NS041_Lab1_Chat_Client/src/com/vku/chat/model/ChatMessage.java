package com.vku.chat.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        TEXT,
        EMOJI,
        FILE,
        AUDIO,
        VIDEO,
        BROADCAST,
        MULTICAST,
        PRIVATE,
        SYSTEM,
        LOGIN,
        LOGOUT,
        USER_LIST,
        ROOM_LIST,
        JOIN_ROOM,
        LEAVE_ROOM,
        CALL_REQUEST,
        CALL_ACCEPT,
        CALL_REJECT,
        CALL_END
    }

    private String id;
    private Type type;
    private String sender;
    private String recipient;   // target user (for PRIVATE) or room name (for MULTICAST/ROOM)
    private String room;        // room identifier
    private String content;     // text message, emoji code, or description
    private long timestamp;
    private String fileName;
    private byte[] fileData;
    private int audioDurationSec;
    private String mediaMeta;

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
        this.id = "MSG-" + this.timestamp + "-" + (int)(Math.random() * 1000);
    }

    public ChatMessage(Type type, String sender, String content) {
        this();
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    public static ChatMessage text(String sender, String recipient, String content, boolean isPrivate) {
        ChatMessage msg = new ChatMessage(isPrivate ? Type.PRIVATE : Type.BROADCAST, sender, content);
        msg.setRecipient(recipient);
        return msg;
    }

    public static ChatMessage roomMessage(String sender, String room, String content) {
        ChatMessage msg = new ChatMessage(Type.MULTICAST, sender, content);
        msg.setRoom(room);
        msg.setRecipient(room);
        return msg;
    }

    public static ChatMessage file(String sender, String recipient, String room, String fileName, byte[] data) {
        ChatMessage msg = new ChatMessage(Type.FILE, sender, "Đã gửi tệp: " + fileName);
        msg.setRecipient(recipient);
        msg.setRoom(room);
        msg.setFileName(fileName);
        msg.setFileData(data);
        return msg;
    }

    public static ChatMessage audio(String sender, String recipient, String room, byte[] audioData, int durationSec) {
        ChatMessage msg = new ChatMessage(Type.AUDIO, sender, "Tin nhắn thoại (" + durationSec + "s)");
        msg.setRecipient(recipient);
        msg.setRoom(room);
        msg.setFileData(audioData);
        msg.setAudioDurationSec(durationSec);
        return msg;
    }

    public static ChatMessage video(String sender, String recipient, String room, byte[] videoData, String meta) {
        ChatMessage msg = new ChatMessage(Type.VIDEO, sender, "Tin nhắn video [" + meta + "]");
        msg.setRecipient(recipient);
        msg.setRoom(room);
        msg.setFileData(videoData);
        msg.setMediaMeta(meta);
        return msg;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(Type.SYSTEM, "HỆ THỐNG", content);
    }

    public static ChatMessage callRequest(String sender, String recipient, String callType) {
        ChatMessage msg = new ChatMessage(Type.CALL_REQUEST, sender, callType != null ? callType : "VIDEO");
        msg.setRecipient(recipient);
        return msg;
    }

    public static ChatMessage callAccept(String sender, String recipient) {
        ChatMessage msg = new ChatMessage(Type.CALL_ACCEPT, sender, "ACCEPT");
        msg.setRecipient(recipient);
        return msg;
    }

    public static ChatMessage callReject(String sender, String recipient, String reason) {
        ChatMessage msg = new ChatMessage(Type.CALL_REJECT, sender, reason != null ? reason : "REJECT");
        msg.setRecipient(recipient);
        return msg;
    }

    public static ChatMessage callEnd(String sender, String recipient) {
        ChatMessage msg = new ChatMessage(Type.CALL_END, sender, "END");
        msg.setRecipient(recipient);
        return msg;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public int getAudioDurationSec() { return audioDurationSec; }
    public void setAudioDurationSec(int audioDurationSec) { this.audioDurationSec = audioDurationSec; }

    public String getMediaMeta() { return mediaMeta; }
    public void setMediaMeta(String mediaMeta) { this.mediaMeta = mediaMeta; }

    public String getFormattedTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (%s): %s",
                getFormattedTime(), sender, (recipient != null ? recipient : "ALL"), type, content);
    }
}
