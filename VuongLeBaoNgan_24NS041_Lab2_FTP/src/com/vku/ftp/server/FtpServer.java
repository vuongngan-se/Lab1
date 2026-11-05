package com.vku.ftp.server;

import com.vku.ftp.db.FtpDatabase;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FtpServer {
    public static final int DEFAULT_PORT = 2121;

    private final int port;
    private final FtpDatabase database;
    private final List<FtpCommandHandler> handlers = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private boolean running = false;
    private FtpServerGUI gui;

    public FtpServer() {
        this(DEFAULT_PORT);
    }

    public FtpServer(int port) {
        this.port = port;
        this.database = new FtpDatabase();
    }

    public void setGui(FtpServerGUI gui) {
        this.gui = gui;
    }

    public void start() {
        if (running) return;
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("FTP Server đã khởi động trên cổng: " + port + " (RFC 959)");
                log("Tương thích hoàn toàn với FileZilla Client và dòng lệnh FTP.");

                while (running && !serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    FtpCommandHandler handler = new FtpCommandHandler(clientSocket, this, database);
                    handlers.add(handler);
                    threadPool.submit(handler);
                    updateClientCount();
                }
            } catch (IOException e) {
                if (running) log("Lỗi FTP ServerSocket: " + e.getMessage());
            }
        }, "FTP-Server-Acceptor").start();
    }

    public void stop() {
        running = false;
        try {
            for (FtpCommandHandler h : handlers) h.close();
            handlers.clear();
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
    }

    public void onClientDisconnected(FtpCommandHandler handler) {
        handlers.remove(handler);
        updateClientCount();
    }

    public void updateClientCount() {
        if (gui != null) {
            gui.updateClientCount(handlers.size());
            gui.refreshData();
        }
    }

    public void log(String msg) {
        System.out.println(msg);
        if (gui != null) {
            gui.appendLog(msg);
        }
    }

    public FtpDatabase getDatabase() { return database; }
    public int getPort() { return port; }
    public boolean isRunning() { return running; }
}
