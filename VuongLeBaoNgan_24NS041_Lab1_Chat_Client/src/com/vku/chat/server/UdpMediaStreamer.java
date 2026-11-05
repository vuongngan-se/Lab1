package com.vku.chat.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class UdpMediaStreamer {
    public static final int AUDIO_STREAM_PORT = 8891;
    public static final int VIDEO_STREAM_PORT = 8892;

    private DatagramSocket socket;
    private boolean running = false;
    private Thread receiverThread;
    private BiConsumer<String, byte[]> onFrameReceived;
    private final Map<String, InetSocketAddress> clientEndpoints = new ConcurrentHashMap<>();

    public UdpMediaStreamer(int port, BiConsumer<String, byte[]> onFrameReceived) {
        this.onFrameReceived = onFrameReceived;
        try {
            this.socket = new DatagramSocket(port);
        } catch (Exception e) {
            System.err.println("Không thể bind cổng UDP Media " + port + ": " + e.getMessage());
        }
    }

    public void startListening() {
        if (socket == null || running) return;
        running = true;
        receiverThread = new Thread(() -> {
            byte[] buf = new byte[65507]; // Max UDP packet size
            while (running && !socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                    // Check if it's a MediaPacket for video/audio relay
                    MediaPacket mp = MediaPacket.decode(data, data.length);
                    if (mp != null) {
                        String sender = mp.getSender().trim().toLowerCase();
                        clientEndpoints.put(sender, new InetSocketAddress(packet.getAddress(), packet.getPort()));

                        if (mp.getType() == MediaPacket.TYPE_VIDEO_FRAME || mp.getType() == MediaPacket.TYPE_AUDIO_CHUNK) {
                            String target = mp.getTarget().trim().toLowerCase();
                            InetSocketAddress targetAddr = clientEndpoints.get(target);
                            if (targetAddr != null) {
                                sendTo(targetAddr.getAddress(), targetAddr.getPort(), data);
                            }
                        }
                    }

                    String senderInfo = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    if (onFrameReceived != null) {
                        onFrameReceived.accept(senderInfo, data);
                    }
                } catch (Exception e) {
                    if (!running) break;
                }
            }
        }, "UDP-Media-Listener");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public void sendTo(InetAddress addr, int port, byte[] data) {
        if (socket == null || socket.isClosed()) return;
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Lỗi forward UDP Media: " + e.getMessage());
        }
    }

    public static void sendFrame(String targetHost, int targetPort, byte[] frameData) {
        try (DatagramSocket sender = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(
                    frameData, frameData.length, InetAddress.getByName(targetHost), targetPort
            );
            sender.send(packet);
        } catch (Exception e) {
            System.err.println("Lỗi gửi gói UDP Media: " + e.getMessage());
        }
    }

    public Map<String, InetSocketAddress> getClientEndpoints() {
        return clientEndpoints;
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
