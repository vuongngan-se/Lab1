package com.vku.ftp.server;

import com.vku.ftp.common.FtpLog;
import com.vku.ftp.common.FtpUser;
import com.vku.ftp.db.FtpDatabase;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FtpCommandHandler implements Runnable {
    private final Socket controlSocket;
    private final FtpServer server;
    private final FtpDatabase db;

    private BufferedReader reader;
    private BufferedWriter writer;
    private FtpUser currentUser;
    private String currentUsernameAttempt;
    private File currentDir;
    private File rootDir;
    private boolean active = true;

    // Data connection
    private ServerSocket passiveServerSocket;
    private String activeHost;
    private int activePort = -1;
    private boolean isPassive = false;

    public FtpCommandHandler(Socket controlSocket, FtpServer server, FtpDatabase db) {
        this.controlSocket = controlSocket;
        this.server = server;
        this.db = db;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), "UTF-8"));

            sendReply("220 VKU FTP Server ready (RFC 959).");

            String line;
            while (active && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                server.log("[" + getClientIp() + "] RECV: " + line);
                handleCommand(line);
            }
        } catch (Exception e) {
            // Client closed
        } finally {
            close();
        }
    }

    private void handleCommand(String fullCommand) throws IOException {
        String[] parts = fullCommand.split(" ", 2);
        String cmd = parts[0].toUpperCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "USER":
                currentUsernameAttempt = arg;
                sendReply("331 Password required for " + arg);
                break;

            case "PASS":
                FtpUser auth = db.authenticate(currentUsernameAttempt, arg);
                if (auth != null) {
                    this.currentUser = auth;
                    this.rootDir = new File(auth.getHomeDirectory()).getAbsoluteFile();
                    if (!this.rootDir.exists()) this.rootDir.mkdirs();
                    this.currentDir = this.rootDir;
                    db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "LOGIN", "", "SUCCESS", 0));
                    sendReply("230 User " + auth.getUsername() + " logged in, proceed.");
                    server.updateClientCount();
                } else {
                    db.logAction(new FtpLog(getClientIp(), currentUsernameAttempt, "LOGIN", "", "FAILED", 0));
                    sendReply("530 Not logged in, user or password incorrect.");
                }
                break;

            case "SYST":
                sendReply("215 UNIX Type: L8");
                break;

            case "FEAT":
                sendReply("211-Features:\r\n PASV\r\n SIZE\r\n UTF8\r\n211 End");
                break;

            case "PWD":
            case "XPWD":
                if (!checkAuth()) return;
                sendReply("257 \"" + getRelativePath() + "\" is current directory");
                break;

            case "CWD":
            case "XCWD":
                if (!checkAuth()) return;
                changeDirectory(arg);
                break;

            case "CDUP":
            case "XCUP":
                if (!checkAuth()) return;
                changeDirectory("..");
                break;

            case "TYPE":
                sendReply("200 Type set to " + arg);
                break;

            case "PASV":
                if (!checkAuth()) return;
                setupPassiveMode();
                break;

            case "PORT":
                if (!checkAuth()) return;
                setupActiveMode(arg);
                break;

            case "LIST":
            case "NLST":
                if (!checkAuth()) return;
                handleList();
                break;

            case "RETR":
                if (!checkAuth()) return;
                handleRetr(arg);
                break;

            case "STOR":
                if (!checkAuth()) return;
                handleStor(arg);
                break;

            case "DELE":
                if (!checkAuth()) return;
                handleDele(arg);
                break;

            case "MKD":
            case "XMKD":
                if (!checkAuth()) return;
                handleMkd(arg);
                break;

            case "RMD":
            case "XRMD":
                if (!checkAuth()) return;
                handleRmd(arg);
                break;

            case "SIZE":
                if (!checkAuth()) return;
                File f = resolveFile(arg);
                if (f.exists() && f.isFile()) {
                    sendReply("213 " + f.length());
                } else {
                    sendReply("550 File not found");
                }
                break;

            case "NOOP":
                sendReply("200 OK");
                break;

            case "QUIT":
                sendReply("221 Goodbye.");
                active = false;
                break;

            default:
                sendReply("502 Command not implemented.");
                break;
        }
    }

    private boolean checkAuth() throws IOException {
        if (currentUser == null) {
            sendReply("530 Please login with USER and PASS first.");
            return false;
        }
        return true;
    }

    private void changeDirectory(String target) throws IOException {
        File next;
        if (target.startsWith("/") || target.startsWith("\\")) {
            next = new File(rootDir, target.substring(1)).getCanonicalFile();
        } else {
            next = new File(currentDir, target).getCanonicalFile();
        }

        if (next.exists() && next.isDirectory() && isUnderRoot(next)) {
            currentDir = next;
            sendReply("250 Directory successfully changed to \"" + getRelativePath() + "\"");
        } else {
            sendReply("550 Failed to change directory.");
        }
    }

    private void setupPassiveMode() throws IOException {
        if (passiveServerSocket != null && !passiveServerSocket.isClosed()) {
            passiveServerSocket.close();
        }
        passiveServerSocket = new ServerSocket(0, 1, controlSocket.getLocalAddress());
        isPassive = true;

        InetAddress localAddr = controlSocket.getLocalAddress();
        byte[] ip = localAddr.getAddress();
        int port = passiveServerSocket.getLocalPort();
        int p1 = port / 256;
        int p2 = port % 256;

        String ipStr = String.format("%d,%d,%d,%d",
                ip[0] & 0xFF, ip[1] & 0xFF, ip[2] & 0xFF, ip[3] & 0xFF);
        sendReply(String.format("227 Entering Passive Mode (%s,%d,%d)", ipStr, p1, p2));
    }

    private void setupActiveMode(String arg) throws IOException {
        String[] parts = arg.split(",");
        if (parts.length == 6) {
            activeHost = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
            activePort = (Integer.parseInt(parts[4]) * 256) + Integer.parseInt(parts[5]);
            isPassive = false;
            sendReply("200 PORT command successful.");
        } else {
            sendReply("501 Syntax error in parameters.");
        }
    }

    private Socket openDataConnection() throws IOException {
        if (isPassive) {
            if (passiveServerSocket == null || passiveServerSocket.isClosed()) {
                throw new IOException("Passive socket is not open");
            }
            Socket s = passiveServerSocket.accept();
            passiveServerSocket.close();
            passiveServerSocket = null;
            return s;
        } else {
            if (activeHost == null || activePort <= 0) {
                throw new IOException("PORT was not specified");
            }
            return new Socket(activeHost, activePort);
        }
    }

    private void handleList() throws IOException {
        sendReply("150 Here comes the directory listing.");
        try (Socket dataSocket = openDataConnection();
             BufferedWriter dataWriter = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream(), "UTF-8"))) {

            File[] files = currentDir.listFiles();
            if (files != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm", Locale.ENGLISH);
                for (File f : files) {
                    String perms = (f.isDirectory() ? "d" : "-") + "rwxr-xr-x";
                    String line = String.format("%s 1 owner group %10d %s %s\r\n",
                            perms, f.length(), sdf.format(new Date(f.lastModified())), f.getName());
                    dataWriter.write(line);
                }
            }
            dataWriter.flush();
            db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "LIST", getRelativePath(), "SUCCESS", 0));
            sendReply("226 Directory send OK.");
        } catch (Exception e) {
            db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "LIST", getRelativePath(), "FAILED", 0));
            sendReply("425 Can't open data connection: " + e.getMessage());
        }
    }

    private void handleRetr(String filename) throws IOException {
        File file = resolveFile(filename);
        if (!file.exists() || !file.isFile()) {
            sendReply("550 File not found.");
            return;
        }

        sendReply("150 Opening BINARY mode data connection for " + file.getName() + " (" + file.length() + " bytes).");
        try (Socket dataSocket = openDataConnection();
             FileInputStream fis = new FileInputStream(file);
             OutputStream os = dataSocket.getOutputStream()) {

            byte[] buf = new byte[8192];
            int read;
            long total = 0;
            while ((read = fis.read(buf)) != -1) {
                os.write(buf, 0, read);
                total += read;
            }
            os.flush();
            db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "DOWNLOAD", file.getName(), "SUCCESS", total));
            sendReply("226 Transfer complete.");
        } catch (Exception e) {
            db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "DOWNLOAD", file.getName(), "FAILED", 0));
            sendReply("426 Connection closed; transfer aborted.");
        }
    }

    private void handleStor(String filename) throws IOException {
        if (!currentUser.isCanWrite()) {
            sendReply("550 Permission denied (No write permission).");
            return;
        }

        File file = resolveFile(filename);
        sendReply("150 Ok to send data.");
        try (Socket dataSocket = openDataConnection();
             FileOutputStream fos = new FileOutputStream(file);
             InputStream is = dataSocket.getInputStream()) {

            byte[] buf = new byte[8192];
            int read;
            long total = 0;
            while ((read = is.read(buf)) != -1) {
                fos.write(buf, 0, read);
                total += read;
            }
            fos.flush();
            db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "UPLOAD", file.getName(), "SUCCESS", total));
            sendReply("226 Transfer complete.");
        } catch (Exception e) {
            db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "UPLOAD", file.getName(), "FAILED", 0));
            sendReply("426 Connection closed; transfer aborted.");
        }
    }

    private void handleDele(String filename) throws IOException {
        if (!currentUser.isCanDelete()) {
            sendReply("550 Permission denied (No delete permission).");
            return;
        }
        File file = resolveFile(filename);
        if (file.exists() && file.isFile() && file.delete()) {
            db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "DELETE", file.getName(), "SUCCESS", 0));
            sendReply("250 File deleted successfully.");
        } else {
            sendReply("550 Could not delete file.");
        }
    }

    private void handleMkd(String dirName) throws IOException {
        if (!currentUser.isCanWrite()) {
            sendReply("550 Permission denied.");
            return;
        }
        File dir = resolveFile(dirName);
        if (dir.mkdirs()) {
            db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "MKD", dirName, "SUCCESS", 0));
            sendReply("257 \"" + dirName + "\" created.");
        } else {
            sendReply("550 Could not create directory.");
        }
    }

    private void handleRmd(String dirName) throws IOException {
        if (!currentUser.isCanDelete()) {
            sendReply("550 Permission denied.");
            return;
        }
        File dir = resolveFile(dirName);
        if (dir.exists() && dir.isDirectory() && dir.delete()) {
            db.logAction(new FtpLog(getClientIp(), currentUser.getUsername(), "RMD", dirName, "SUCCESS", 0));
            sendReply("250 Directory removed.");
        } else {
            sendReply("550 Could not remove directory.");
        }
    }

    private File resolveFile(String path) throws IOException {
        if (path.startsWith("/") || path.startsWith("\\")) {
            return new File(rootDir, path.substring(1)).getCanonicalFile();
        }
        return new File(currentDir, path).getCanonicalFile();
    }

    private boolean isUnderRoot(File f) {
        return f.getAbsolutePath().startsWith(rootDir.getAbsolutePath());
    }

    private String getRelativePath() {
        String r = rootDir.getAbsolutePath();
        String c = currentDir.getAbsolutePath();
        if (c.equals(r)) return "/";
        String rel = c.substring(r.length()).replace('\\', '/');
        return rel.startsWith("/") ? rel : "/" + rel;
    }

    private void sendReply(String reply) throws IOException {
        server.log("[" + getClientIp() + "] SEND: " + reply.replace("\r\n", " "));
        writer.write(reply + "\r\n");
        writer.flush();
    }

    private String getClientIp() {
        return controlSocket.getInetAddress().getHostAddress();
    }

    public void close() {
        active = false;
        try {
            if (passiveServerSocket != null && !passiveServerSocket.isClosed()) passiveServerSocket.close();
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (controlSocket != null && !controlSocket.isClosed()) controlSocket.close();
        } catch (Exception ignored) {}
        server.onClientDisconnected(this);
    }

    public FtpUser getCurrentUser() { return currentUser; }
}
