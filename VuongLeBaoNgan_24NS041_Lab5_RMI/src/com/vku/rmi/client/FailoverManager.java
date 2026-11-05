package com.vku.rmi.client;

import com.vku.rmi.service.IClientService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class FailoverManager {
    public static class ServerEndpoint {
        public String name;
        public String host;
        public int port;

        public ServerEndpoint(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return name + " (" + host + ":" + port + ")";
        }
    }

    private final ServerEndpoint primary;
    private final ServerEndpoint secondary;
    private ServerEndpoint activeEndpoint;
    private IClientService activeService;

    public FailoverManager(String host1, int port1, String host2, int port2) {
        this.primary = new ServerEndpoint("SERVER_1", host1, port1);
        this.secondary = new ServerEndpoint("SERVER_2", host2, port2);
        this.activeEndpoint = primary;
    }

    public synchronized IClientService getService() {
        if (activeService != null) {
            try {
                // Ping active service
                activeService.getServerId();
                return activeService;
            } catch (Exception e) {
                System.out.println("⚠ Mất kết nối tới " + activeEndpoint + "! Bắt đầu quá trình Failover tự động...");
                activeService = null;
            }
        }

        // Try primary
        try {
            Registry reg = LocateRegistry.getRegistry(primary.host, primary.port);
            activeService = (IClientService) reg.lookup("ClientService");
            activeEndpoint = primary;
            System.out.println("✓ Đã kết nối thành công tới Server chính: " + primary);
            return activeService;
        } catch (Exception e) {
            System.out.println("Không thể kết nối Server chính " + primary + ", chuyển sang Server dự phòng...");
        }

        // Try secondary
        try {
            Registry reg = LocateRegistry.getRegistry(secondary.host, secondary.port);
            activeService = (IClientService) reg.lookup("ClientService");
            activeEndpoint = secondary;
            System.out.println("✓ [FAILOVER THÀNH CÔNG] Đã chuyển đổi sang Server phụ: " + secondary);
            return activeService;
        } catch (Exception e) {
            System.out.println("Không thể kết nối tới cả 2 Server: " + e.getMessage());
        }

        return null;
    }

    public void forceSwitchServer() {
        activeService = null;
        activeEndpoint = (activeEndpoint == primary) ? secondary : primary;
    }

    public ServerEndpoint getActiveEndpoint() {
        return activeEndpoint;
    }
}
