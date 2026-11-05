package com.vku.mail.server;

import com.vku.mail.db.MailDatabase;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailServer {
    public static final int SMTP_PORT = 2525;
    public static final int POP3_PORT = 1100;

    private final MailDatabase database;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private ServerSocket smtpSocket;
    private ServerSocket pop3Socket;
    private boolean running = false;
    private MailServerGUI gui;

    public MailServer() {
        this.database = new MailDatabase();
    }

    public void setGui(MailServerGUI gui) {
        this.gui = gui;
    }

    public void start() {
        if (running) return;
        running = true;

        // Start SMTP listener
        new Thread(() -> {
            try {
                smtpSocket = new ServerSocket(SMTP_PORT);
                log("SMTP Server đang lắng nghe trên cổng: " + SMTP_PORT + " (RFC 5321)");
                while (running && !smtpSocket.isClosed()) {
                    Socket client = smtpSocket.accept();
                    threadPool.submit(new SmtpServerHandler(client, this, database));
                }
            } catch (IOException e) {
                if (running) log("Lỗi SMTP ServerSocket: " + e.getMessage());
            }
        }, "SMTP-Acceptor").start();

        // Start POP3 listener
        new Thread(() -> {
            try {
                pop3Socket = new ServerSocket(POP3_PORT);
                log("POP3 Server đang lắng nghe trên cổng: " + POP3_PORT + " (RFC 1939)");
                while (running && !pop3Socket.isClosed()) {
                    Socket client = pop3Socket.accept();
                    threadPool.submit(new Pop3ServerHandler(client, this, database));
                }
            } catch (IOException e) {
                if (running) log("Lỗi POP3 ServerSocket: " + e.getMessage());
            }
        }, "POP3-Acceptor").start();
    }

    public void stop() {
        running = false;
        try {
            if (smtpSocket != null && !smtpSocket.isClosed()) smtpSocket.close();
            if (pop3Socket != null && !pop3Socket.isClosed()) pop3Socket.close();
        } catch (Exception ignored) {}
    }

    public void notifyNewMail() {
        if (gui != null) {
            gui.refreshData();
        }
    }

    public void log(String msg) {
        System.out.println(msg);
        if (gui != null) {
            gui.appendLog(msg);
        }
    }

    public MailDatabase getDatabase() { return database; }
    public boolean isRunning() { return running; }
}
