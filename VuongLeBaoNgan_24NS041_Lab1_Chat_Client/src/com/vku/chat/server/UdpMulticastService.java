package com.vku.chat.server;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class UdpMulticastService {
    public static final String DEFAULT_MULTICAST_GROUP = "224.2.2.3";
    public static final int DEFAULT_MULTICAST_PORT = 8889;
    public static final int DEFAULT_BROADCAST_PORT = 8890;

    private MulticastSocket multicastSocket;
    private InetSocketAddress groupAddress;
    private NetworkInterface networkInterface;
    private boolean running = false;
    private Thread listenerThread;
    private Consumer<String> onMessageReceived;

    public UdpMulticastService(String groupIp, int port, Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
        try {
            this.groupAddress = new InetSocketAddress(InetAddress.getByName(groupIp), port);
            this.networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (this.networkInterface == null) {
                // Fallback to first active interface
                var interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    var ni = interfaces.nextElement();
                    if (ni.isUp() && !ni.isLoopback()) {
                        this.networkInterface = ni;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Cảnh báo khởi tạo UDP Multicast: " + e.getMessage());
        }
    }

    public void start() {
        if (running) return;
        running = true;
        listenerThread = new Thread(() -> {
            try {
                multicastSocket = new MulticastSocket(groupAddress.getPort());
                if (networkInterface != null) {
                    multicastSocket.joinGroup(groupAddress, networkInterface);
                }
                byte[] buffer = new byte[4096];
                while (running && !multicastSocket.isClosed()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(msg);
                    }
                }
            } catch (SocketException se) {
                if (!running) return;
                System.err.println("UDP Multicast socket đóng: " + se.getMessage());
            } catch (Exception e) {
                if (running) System.err.println("Lỗi UDP Multicast: " + e.getMessage());
            }
        }, "UDP-Multicast-Receiver");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void sendMulticast(String message) {
        try {
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(
                    bytes, bytes.length, groupAddress.getAddress(), groupAddress.getPort()
            );
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.send(packet);
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi UDP Multicast: " + e.getMessage());
        }
    }

    public static void sendUdpBroadcast(String message, int port) {
        try {
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                DatagramPacket packet = new DatagramPacket(
                        bytes, bytes.length, InetAddress.getByName("255.255.255.255"), port
                );
                socket.send(packet);
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi UDP Broadcast: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                if (networkInterface != null) {
                    multicastSocket.leaveGroup(groupAddress, networkInterface);
                }
            } catch (Exception ignored) {}
            multicastSocket.close();
        }
    }
}
