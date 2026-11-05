package com.vku.rmi.service;

import com.vku.rmi.model.DataRecord;
import com.vku.rmi.model.ServerInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IDataSyncService extends Remote {
    /**
     * Replicates creation of a new record from the originating server.
     */
    boolean syncCreate(DataRecord record, String originServerId) throws RemoteException;

    /**
     * Replicates update of an existing record from the originating server.
     */
    boolean syncUpdate(DataRecord record, String originServerId) throws RemoteException;

    /**
     * Replicates deletion of a record from the originating server.
     */
    boolean syncDelete(String recordId, String originServerId) throws RemoteException;

    /**
     * Retrieves all records modified after a given timestamp (for delta/catch-up sync).
     */
    List<DataRecord> syncAll(long lastSyncTimestamp) throws RemoteException;

    /**
     * Heartbeat check between Server 1 and Server 2.
     */
    boolean heartbeat(String senderServerId) throws RemoteException;

    /**
     * Retrieves server operational status.
     */
    ServerInfo getServerInfo() throws RemoteException;
}
