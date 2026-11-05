package com.vku.rmi.server;

import com.vku.rmi.db.ServerDatabase;
import com.vku.rmi.model.DataRecord;
import com.vku.rmi.model.ServerInfo;
import com.vku.rmi.service.IClientService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ClientServiceImpl extends UnicastRemoteObject implements IClientService {
    private static final long serialVersionUID = 1L;

    private final RmiSyncServer server;
    private final ServerDatabase db;

    public ClientServiceImpl(RmiSyncServer server, ServerDatabase db) throws RemoteException {
        super();
        this.server = server;
        this.db = db;
    }

    @Override
    public List<DataRecord> getAllRecords() throws RemoteException {
        return db.getAllRecords();
    }

    @Override
    public DataRecord getRecordById(String id) throws RemoteException {
        return db.getRecord(id);
    }

    @Override
    public boolean createRecord(DataRecord record) throws RemoteException {
        server.log("[CLIENT THAO TÁC] Thêm bản ghi: " + record.getId() + " - " + record.getName());
        record.setVersion(1);
        record.setUpdatedByServer(server.getServerId());
        boolean ok = db.saveOrUpdateRecordLocal(record);

        // Asynchronously replicate to partner server via RMI
        server.replicateCreate(record);
        return ok;
    }

    @Override
    public boolean updateRecord(DataRecord record) throws RemoteException {
        server.log("[CLIENT THAO TÁC] Cập nhật bản ghi: " + record.getId());
        DataRecord existing = db.getRecord(record.getId());
        long newVer = (existing != null) ? existing.getVersion() + 1 : 1;
        record.setVersion(newVer);
        record.setUpdatedByServer(server.getServerId());
        boolean ok = db.saveOrUpdateRecordLocal(record);

        // Replicate to partner server via RMI
        server.replicateUpdate(record);
        return ok;
    }

    @Override
    public boolean deleteRecord(String id) throws RemoteException {
        server.log("[CLIENT THAO TÁC] Xóa bản ghi: " + id);
        boolean ok = db.deleteRecordLocal(id);

        // Replicate to partner server via RMI
        server.replicateDelete(id);
        return ok;
    }

    @Override
    public String getServerId() throws RemoteException {
        return server.getServerId();
    }

    @Override
    public ServerInfo getServerInfo() throws RemoteException {
        return server.buildServerInfo();
    }
}
