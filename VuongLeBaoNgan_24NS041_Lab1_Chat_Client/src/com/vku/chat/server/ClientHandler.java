package com.vku.chat.server;

import com.vku.chat.model.ChatMessage;
import com.vku.chat.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user;
    private final Set<String> joinedRooms = new HashSet<>();
    private boolean active = true;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.joinedRooms.add("general"); // Default join general room
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Authentication loop
            while (active) {
                Object obj = in.readObject();
                if (!(obj instanceof ChatMessage)) continue;
                ChatMessage msg = (ChatMessage) obj;

                if (msg.getType() == ChatMessage.Type.LOGIN) {
                    handleLogin(msg);
                    if (user != null) break; // authenticated
                } else {
                    sendMessage(ChatMessage.system("Vui lòng đăng nhập trước khi tiếp tục!"));
                }
            }

            if (user == null) {
                close();
                return;
            }

            // Main message handling loop
            while (active) {
                Object obj = in.readObject();
                if (!(obj instanceof ChatMessage)) continue;
                ChatMessage msg = (ChatMessage) obj;
                msg.setSender(user.getUsername());
                msg.setTimestamp(System.currentTimeMillis());

                switch (msg.getType()) {
                    case LOGOUT:
                        active = false;
                        break;
                    case BROADCAST:
                        server.broadcastMessage(msg);
                        break;
                    case MULTICAST:
                        server.sendToRoom(msg);
                        break;
                    case PRIVATE:
                        server.sendPrivateMessage(msg);
                        break;
                    case CALL_REQUEST:
                    case CALL_ACCEPT:
                    case CALL_REJECT:
                    case CALL_END:
                        server.handleCallSignaling(msg);
                        break;
                    case FILE:
                    case AUDIO:
                    case VIDEO:
                    case EMOJI:
                    case TEXT:
                        if (msg.getRecipient() != null && !msg.getRecipient().equalsIgnoreCase("ALL")
                                && !msg.getRecipient().equalsIgnoreCase("general")) {
                            if (server.isRoom(msg.getRecipient())) {
                                msg.setRoom(msg.getRecipient());
                                server.sendToRoom(msg);
                            } else {
                                server.sendPrivateMessage(msg);
                            }
                        } else {
                            server.broadcastMessage(msg);
                        }
                        break;
                    case JOIN_ROOM:
                        joinedRooms.add(msg.getRoom());
                        server.log("User " + user.getUsername() + " tham gia phòng " + msg.getRoom());
                        sendMessage(ChatMessage.system("Bạn đã tham gia phòng: " + msg.getRoom()));
                        break;
                    case LEAVE_ROOM:
                        joinedRooms.remove(msg.getRoom());
                        server.log("User " + user.getUsername() + " rời phòng " + msg.getRoom());
                        sendMessage(ChatMessage.system("Bạn đã rời phòng: " + msg.getRoom()));
                        break;
                    default:
                        server.broadcastMessage(msg);
                        break;
                }
            }
        } catch (Exception e) {
            // Client disconnected
        } finally {
            close();
        }
    }

    private void handleLogin(ChatMessage msg) throws IOException {
        String[] parts = msg.getContent().split(":", 2);
        String username = parts[0];
        String password = parts.length > 1 ? parts[1] : "";

        User auth = server.getDatabase().authenticate(username, password);
        if (auth != null) {
            this.user = auth;
            ChatMessage success = new ChatMessage(ChatMessage.Type.LOGIN, "SERVER", "SUCCESS:" + auth.getFullName());
            sendMessage(success);
            server.onClientConnected(this);
        } else {
            ChatMessage fail = new ChatMessage(ChatMessage.Type.LOGIN, "SERVER", "FAILED:Sai tên đăng nhập hoặc mật khẩu!");
            sendMessage(fail);
        }
    }

    public synchronized void sendMessage(ChatMessage message) {
        if (!active || out == null) return;
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            active = false;
        }
    }

    public void close() {
        active = false;
        try {
            if (user != null) {
                server.onClientDisconnected(this);
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }

    public User getUser() { return user; }
    public String getUsername() { return user != null ? user.getUsername() : "Unknown"; }
    public boolean isInRoom(String roomId) {
        if (roomId == null || roomId.equalsIgnoreCase("general")) return true;
        return joinedRooms.contains(roomId);
    }
}
