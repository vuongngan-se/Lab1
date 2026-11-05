package com.vku.rmi.server;

import com.vku.rmi.db.ServerDatabase;
import com.vku.rmi.model.DataRecord;
import com.vku.rmi.model.ServerInfo;
import com.vku.rmi.model.SyncEvent;
import com.vku.rmi.service.IDataSyncService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class DataSyncServiceImpl extends UnicastRemoteObject implements IDataSyncService {
    private static final long serialVersionUID = 1L;

    private final RmiSyncServer server;
    private final ServerDatabase db;

    public DataSyncServiceImpl(RmiSyncServer server, ServerDatabase db) throws RemoteException {
        super();
        this.server = server;
        this.db = db;
    }

    @Override
    public boolean syncCreate(DataRecord record, String originServerId) throws RemoteException {
        server.log("<- [RMI SYNC NHẬN] Tạo bản ghi mới từ " + originServerId + ": " + record.getId());
        boolean ok = db.applyReplication(record, originServerId);
        db.recordEvent(new SyncEvent(SyncEvent.Type.CREATE, record.getId(), originServerId, server.getServerId(), "Đồng bộ tạo mới"));
        return ok;
    }

    @Override
    public boolean syncUpdate(DataRecord record, String originServerId) throws RemoteException {
        server.log("<- [RMI SYNC NHẬN] Cập nhật bản ghi từ " + originServerId + ": " + record.getId());
        boolean ok = db.applyReplication(record, originServerId);
        db.recordEvent(new SyncEvent(SyncEvent.Type.UPDATE, record.getId(), originServerId, server.getServerId(), "Đồng bộ cập nhật"));
        return ok;
    }

    @Override
    public boolean syncDelete(String recordId, String originServerId) throws RemoteException {
        server.log("<- [RMI SYNC NHẬN] Xóa bản ghi từ " + originServerId + ": " + recordId);
        boolean ok = db.applyReplicationDelete(recordId);
        db.recordEvent(new SyncEvent(SyncEvent.Type.DELETE, recordId, originServerId, server.getServerId(), "Đồng bộ xóa"));
        return ok;
    }

    @Override
    public List<DataRecord> syncAll(long lastSyncTimestamp) throws RemoteException {
        server.log("<- [RMI SYNC NHẬN] Yêu cầu toàn bộ bản ghi đồng bộ từ partner");
        List<DataRecord> list = new ArrayList<>();
        for (DataRecord r : db.getAllRecords()) {
            if (r.getLastModified() >= lastSyncTimestamp) {
                list.add(r);
            }
        }
        db.recordEvent(new SyncEvent(SyncEvent.Type.FULL_SYNC, "ALL", "PARTNER", server.getServerId(), "Truy xuất danh sách đồng bộ"));
        return list;
    }

    @Override
    public boolean heartbeat(String senderServerId) throws RemoteException {
        return true;
    }

    @Override
    public ServerInfo getServerInfo() throws RemoteException {
        return server.buildServerInfo();
    }
}
