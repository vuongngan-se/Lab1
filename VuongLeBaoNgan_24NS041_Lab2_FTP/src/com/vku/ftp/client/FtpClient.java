package com.vku.ftp.client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FtpClient {
    public static class FtpItem {
        public String name;
        public long size;
        public boolean isDirectory;
        public String date;

        public FtpItem(String name, long size, boolean isDirectory, String date) {
            this.name = name;
            this.size = size;
            this.isDirectory = isDirectory;
            this.date = date;
        }

        @Override
        public String toString() {
            return (isDirectory ? "[DIR] " : "") + name + " (" + size + " bytes)";
        }
    }

    private Socket controlSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean connected = false;
    private String currentRemoteDir = "/";
    private Consumer<String> logListener;

    public FtpClient() {}

    public void setLogListener(Consumer<String> listener) {
        this.logListener = listener;
    }

    private void log(String msg) {
        System.out.println(msg);
        if (logListener != null) logListener.accept(msg);
    }

    public boolean connect(String host, int port, String user, String pass) {
        try {
            controlSocket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), "UTF-8"));

            String welcome = readResponse();
            if (!welcome.startsWith("220")) {
                disconnect();
                return false;
            }

            sendCommand("USER " + user);
            String userResp = readResponse();
            if (userResp.startsWith("331")) {
                sendCommand("PASS " + pass);
                String passResp = readResponse();
                if (!passResp.startsWith("230")) {
                    disconnect();
                    return false;
                }
            } else if (!userResp.startsWith("230")) {
                disconnect();
                return false;
            }

            // Set binary mode
            sendCommand("TYPE I");
            readResponse();

            sendCommand("PWD");
            String pwdResp = readResponse();
            parsePwd(pwdResp);

            connected = true;
            return true;
        } catch (Exception e) {
            log("Lỗi kết nối FTP: " + e.getMessage());
            disconnect();
            return false;
        }
    }

    private void parsePwd(String resp) {
        int firstQuote = resp.indexOf("\"");
        int lastQuote = resp.lastIndexOf("\"");
        if (firstQuote != -1 && lastQuote > firstQuote) {
            currentRemoteDir = resp.substring(firstQuote + 1, lastQuote);
        }
    }

    private void sendCommand(String cmd) throws IOException {
        log(">> " + cmd);
        writer.write(cmd + "\r\n");
        writer.flush();
    }

    private String readResponse() throws IOException {
        String line = reader.readLine();
        if (line != null) {
            log("<< " + line);
        }
        return line != null ? line : "";
    }

    private Socket openPassiveDataSocket() throws IOException {
        sendCommand("PASV");
        String resp = readResponse();
        if (!resp.startsWith("227")) {
            throw new IOException("Máy chủ không hỗ trợ PASV: " + resp);
        }

        Pattern p = Pattern.compile("(\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)");
        Matcher m = p.matcher(resp);
        if (!m.find()) {
            throw new IOException("Không thể phân tích phản hồi PASV: " + resp);
        }

        String host = m.group(1) + "." + m.group(2) + "." + m.group(3) + "." + m.group(4);
        int port = (Integer.parseInt(m.group(5)) * 256) + Integer.parseInt(m.group(6));

        // Theo chuẩn FTP bảo mật và chống lệch card mạng (giống FileZilla):
        // Luôn kết nối kênh dữ liệu Data Socket đến cùng địa chỉ IP máy chủ đang kết nối
        String serverHost = controlSocket.getInetAddress().getHostAddress();
        if (host.equals("127.0.0.1") || host.equals("0.0.0.0") || !host.equals(serverHost)) {
            host = serverHost;
        }
        return new Socket(host, port);
    }

    public List<FtpItem> listFiles() throws IOException {
        List<FtpItem> list = new ArrayList<>();
        Socket dataSocket = openPassiveDataSocket();
        sendCommand("LIST");
        String resp = readResponse();
        if (!resp.startsWith("150")) {
            dataSocket.close();
            throw new IOException("Lỗi lệnh LIST: " + resp);
        }

        try (BufferedReader dReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = dReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                boolean isDir = line.startsWith("d");
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 9) {
                    long size = 0;
                    try { size = Long.parseLong(tokens[4]); } catch (Exception ignored) {}
                    String date = tokens[5] + " " + tokens[6] + " " + tokens[7];
                    StringBuilder nameSb = new StringBuilder();
                    for (int i = 8; i < tokens.length; i++) {
                        if (nameSb.length() > 0) nameSb.append(" ");
                        nameSb.append(tokens[i]);
                    }
                    list.add(new FtpItem(nameSb.toString(), size, isDir, date));
                }
            }
        } finally {
            dataSocket.close();
        }
        readResponse(); // Read 226 Transfer complete
        return list;
    }

    public boolean changeDirectory(String dir) throws IOException {
        sendCommand("CWD " + dir);
        String resp = readResponse();
        if (resp.startsWith("250")) {
            sendCommand("PWD");
            parsePwd(readResponse());
            return true;
        }
        return false;
    }

    public boolean downloadFile(String remoteName, File localDest) throws IOException {
        Socket dataSocket = openPassiveDataSocket();
        sendCommand("RETR " + remoteName);
        String resp = readResponse();
        if (!resp.startsWith("150")) {
            dataSocket.close();
            return false;
        }

        try (InputStream is = dataSocket.getInputStream();
             FileOutputStream fos = new FileOutputStream(localDest)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = is.read(buf)) != -1) {
                fos.write(buf, 0, read);
            }
            fos.flush();
        } finally {
            dataSocket.close();
        }
        String completeResp = readResponse();
        return completeResp.startsWith("226");
    }

    public boolean uploadFile(File localFile, String remoteName) throws IOException {
        Socket dataSocket = openPassiveDataSocket();
        sendCommand("STOR " + remoteName);
        String resp = readResponse();
        if (!resp.startsWith("150")) {
            dataSocket.close();
            return false;
        }

        try (FileInputStream fis = new FileInputStream(localFile);
             OutputStream os = dataSocket.getOutputStream()) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                os.write(buf, 0, read);
            }
            os.flush();
        } finally {
            dataSocket.close();
        }
        String completeResp = readResponse();
        return completeResp.startsWith("226");
    }

    public boolean deleteFile(String remoteName) throws IOException {
        sendCommand("DELE " + remoteName);
        String resp = readResponse();
        return resp.startsWith("250");
    }

    public boolean createDirectory(String dirName) throws IOException {
        sendCommand("MKD " + dirName);
        String resp = readResponse();
        return resp.startsWith("257");
    }

    public void disconnect() {
        connected = false;
        try {
            if (writer != null) {
                writer.write("QUIT\r\n");
                writer.flush();
            }
        } catch (Exception ignored) {}
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (controlSocket != null && !controlSocket.isClosed()) controlSocket.close();
        } catch (Exception ignored) {}
    }

    public boolean isConnected() { return connected; }
    public String getCurrentRemoteDir() { return currentRemoteDir; }
}
