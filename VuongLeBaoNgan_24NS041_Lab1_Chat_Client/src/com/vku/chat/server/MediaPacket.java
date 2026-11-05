package com.vku.chat.server;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MediaPacket {
    public static final byte MAGIC_1 = 0x56; // 'V'
    public static final byte MAGIC_2 = 0x4B; // 'K'

    public static final byte TYPE_REGISTER = 1;
    public static final byte TYPE_VIDEO_FRAME = 2;
    public static final byte TYPE_AUDIO_CHUNK = 3;
    public static final byte TYPE_END_STREAM = 4;

    private byte type;
    private String sender;
    private String target;
    private int seq;
    private byte[] payload;

    public MediaPacket(byte type, String sender, String target, int seq, byte[] payload) {
        this.type = type;
        this.sender = sender != null ? sender : "";
        this.target = target != null ? target : "";
        this.seq = seq;
        this.payload = payload != null ? payload : new byte[0];
    }

    public byte getType() { return type; }
    public String getSender() { return sender; }
    public String getTarget() { return target; }
    public int getSeq() { return seq; }
    public byte[] getPayload() { return payload; }

    public byte[] encode() {
        byte[] sBytes = sender.getBytes(StandardCharsets.UTF_8);
        byte[] tBytes = target.getBytes(StandardCharsets.UTF_8);
        int totalLen = 2 + 1 + 1 + sBytes.length + 1 + tBytes.length + 4 + payload.length;
        ByteBuffer buf = ByteBuffer.allocate(totalLen);
        buf.put(MAGIC_1);
        buf.put(MAGIC_2);
        buf.put(type);
        buf.put((byte) sBytes.length);
        buf.put(sBytes);
        buf.put((byte) tBytes.length);
        buf.put(tBytes);
        buf.putInt(seq);
        buf.put(payload);
        return buf.array();
    }

    public static MediaPacket decode(byte[] data, int length) {
        if (data == null || length < 9) return null;
        try {
            ByteBuffer buf = ByteBuffer.wrap(data, 0, length);
            byte m1 = buf.get();
            byte m2 = buf.get();
            if (m1 != MAGIC_1 || m2 != MAGIC_2) return null;
            byte type = buf.get();
            int sLen = buf.get() & 0xFF;
            if (buf.remaining() < sLen + 1) return null;
            byte[] sBytes = new byte[sLen];
            buf.get(sBytes);
            String sender = new String(sBytes, StandardCharsets.UTF_8);

            int tLen = buf.get() & 0xFF;
            if (buf.remaining() < tLen + 4) return null;
            byte[] tBytes = new byte[tLen];
            buf.get(tBytes);
            String target = new String(tBytes, StandardCharsets.UTF_8);

            int seq = buf.getInt();
            byte[] payload = new byte[buf.remaining()];
            buf.get(payload);

            return new MediaPacket(type, sender, target, seq, payload);
        } catch (Exception e) {
            return null;
        }
    }
}

