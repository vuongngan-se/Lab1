package com.vku.rmi.server;

import com.vku.rmi.db.ServerDatabase;
import com.vku.rmi.model.DataRecord;
import com.vku.rmi.model.ServerInfo;
import com.vku.rmi.model.SyncEvent;
import com.vku.rmi.service.IClientService;
import com.vku.rmi.service.IDataSyncService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RmiSyncServer {
    private final String serverId;
    private final String host;
    private final int port;
    private final String partnerHost;
    private final int partnerPort;
    private final String partnerServerId;

    private final ServerDatabase database;
    private final long startTime;
    private Registry registry;
    private IDataSyncService partnerSyncService;
    private boolean partnerConnected = false;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public RmiSyncServer(String serverId, String host, int port, String partnerHost, int partnerPort, String partnerId) {
        this.serverId = serverId;
        this.host = host;
        this.port = port;
        this.partnerHost = partnerHost;
        this.partnerPort = partnerPort;
        this.partnerServerId = partnerId;
        this.startTime = System.currentTimeMillis();
        this.database = new ServerDatabase(serverId, "data_" + serverId.toLowerCase());
    }

    public void start() {
        try {
            // Set RMI server hostname
            System.setProperty("java.rmi.server.hostname", host);

            try {
                registry = LocateRegistry.createRegistry(port);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(port);
            }

            ClientServiceImpl clientService = new ClientServiceImpl(this, database);
            DataSyncServiceImpl dataSyncService = new DataSyncServiceImpl(this, database);

            registry.rebind("ClientService", clientService);
            registry.rebind("DataSyncService", dataSyncService);

            log("=================================================================");
            log("  MÁY CHỦ RMI [" + serverId + "] ĐÃ KHỞI ĐỘNG THÀNH CÔNG");
            log("  Địa chỉ: " + host + ":" + port);
            log("  Máy chủ đồng bộ đối tác (Partner): " + partnerHost + ":" + partnerPort + " (" + partnerServerId + ")");
            log("  Dịch vụ RMI đã đăng ký: ClientService, DataSyncService");
            log("  Số bản ghi hiện tại trong database: " + database.getRecordCount());
            log("=================================================================");

            // Start heartbeat and synchronization monitor
            startPartnerMonitor();

        } catch (Exception e) {
            log("Lỗi khởi động RMI Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startPartnerMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (partnerSyncService == null || !partnerConnected) {
                    Registry partnerRegistry = LocateRegistry.getRegistry(partnerHost, partnerPort);
                    partnerSyncService = (IDataSyncService) partnerRegistry.lookup("DataSyncService");
                }

                boolean alive = partnerSyncService.heartbeat(serverId);
                if (alive) {
                    if (!partnerConnected) {
                        partnerConnected = true;
                        log("✓ ĐÃ KẾT NỐI VỚI MÁY CHỦ ĐỐI TÁC [" + partnerServerId + " @" + partnerHost + ":" + partnerPort + "]");
                        // Trigger catch-up sync
                        performCatchUpSync();
                    }
                }
            } catch (Exception e) {
                if (partnerConnected) {
                    partnerConnected = false;
                    partnerSyncService = null;
                    log("⚠ MẤT KẾT NỐI VỚI MÁY CHỦ ĐỐI TÁC [" + partnerServerId + "]. Chế độ chạy độc lập được kích hoạt!");
                }
            }
        }, 1, 3, TimeUnit.SECONDS);
    }

    private void performCatchUpSync() {
        try {
            log("-> Bắt đầu quá trình đồng bộ bù dữ liệu (Catch-Up Sync) với " + partnerServerId + "...");
            List<DataRecord> remoteRecords = partnerSyncService.syncAll(0);
            int synced = 0;
            for (DataRecord r : remoteRecords) {
                if (database.applyReplication(r, partnerServerId)) {
                    synced++;
                }
            }
            database.recordEvent(new SyncEvent(SyncEvent.Type.FULL_SYNC, "ALL", partnerServerId, serverId, "Đồng bộ bù thành công " + synced + " bản ghi"));
            log("✓ Đã đồng bộ hoàn tất " + synced + " bản ghi từ máy chủ đối tác!");
        } catch (Exception e) {
            log("Lỗi Catch-Up Sync: " + e.getMessage());
        }
    }

    public void replicateCreate(DataRecord record) {
        if (!partnerConnected || partnerSyncService == null) {
            log("⚠ Không thể đồng bộ tức thời: Máy chủ đối tác đang offline. Sẽ tự động bù khi kết nối lại.");
            return;
        }
        new Thread(() -> {
            try {
                log("-> [RMI GỬI] Đồng bộ tạo mới sang " + partnerServerId + ": " + record.getId());
                partnerSyncService.syncCreate(record, serverId);
                database.recordEvent(new SyncEvent(SyncEvent.Type.CREATE, record.getId(), serverId, partnerServerId, "Gửi đồng bộ tạo"));
            } catch (Exception e) {
                log("Lỗi gửi replicateCreate: " + e.getMessage());
                partnerConnected = false;
            }
        }).start();
    }

    public void replicateUpdate(DataRecord record) {
        if (!partnerConnected || partnerSyncService == null) return;
        new Thread(() -> {
            try {
                log("-> [RMI GỬI] Đồng bộ cập nhật sang " + partnerServerId + ": " + record.getId());
                partnerSyncService.syncUpdate(record, serverId);
                database.recordEvent(new SyncEvent(SyncEvent.Type.UPDATE, record.getId(), serverId, partnerServerId, "Gửi đồng bộ sửa"));
            } catch (Exception e) {
                log("Lỗi gửi replicateUpdate: " + e.getMessage());
                partnerConnected = false;
            }
        }).start();
    }

    public void replicateDelete(String recordId) {
        if (!partnerConnected || partnerSyncService == null) return;
        new Thread(() -> {
            try {
                log("-> [RMI GỬI] Đồng bộ xóa sang " + partnerServerId + ": " + recordId);
                partnerSyncService.syncDelete(recordId, serverId);
                database.recordEvent(new SyncEvent(SyncEvent.Type.DELETE, recordId, serverId, partnerServerId, "Gửi đồng bộ xóa"));
            } catch (Exception e) {
                log("Lỗi gửi replicateDelete: " + e.getMessage());
                partnerConnected = false;
            }
        }).start();
    }

    public ServerInfo buildServerInfo() {
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        return new ServerInfo(serverId, host, port, database.getRecordCount(), uptime, partnerConnected);
    }

    public void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        System.out.println("[" + time + "][" + serverId + "] " + msg);
    }

    public String getServerId() { return serverId; }
    public ServerDatabase getDatabase() { return database; }

    public static void main(String[] args) {
        // Defaults: Server 1 on 1099, partner on 1100
        String serverId = "SERVER_1";
        String host = "127.0.0.1";
        int port = 1099;
        String partnerHost = "127.0.0.1";
        int partnerPort = 1100;
        String partnerId = "SERVER_2";

        for (int i = 0; i < args.length; i++) {
            if ("-id".equalsIgnoreCase(args[i]) && i + 1 < args.length) serverId = args[++i];
            else if ("-host".equalsIgnoreCase(args[i]) && i + 1 < args.length) host = args[++i];
            else if ("-port".equalsIgnoreCase(args[i]) && i + 1 < args.length) port = Integer.parseInt(args[++i]);
            else if ("-partnerHost".equalsIgnoreCase(args[i]) && i + 1 < args.length) partnerHost = args[++i];
            else if ("-partnerPort".equalsIgnoreCase(args[i]) && i + 1 < args.length) partnerPort = Integer.parseInt(args[++i]);
            else if ("-partnerId".equalsIgnoreCase(args[i]) && i + 1 < args.length) partnerId = args[++i];
        }

        RmiSyncServer server = new RmiSyncServer(serverId, host, port, partnerHost, partnerPort, partnerId);
        server.start();
    }
}
