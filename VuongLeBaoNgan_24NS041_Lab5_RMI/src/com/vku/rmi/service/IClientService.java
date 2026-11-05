package com.vku.rmi.service;

import com.vku.rmi.model.DataRecord;
import com.vku.rmi.model.ServerInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IClientService extends Remote {
    List<DataRecord> getAllRecords() throws RemoteException;
    DataRecord getRecordById(String id) throws RemoteException;
    boolean createRecord(DataRecord record) throws RemoteException;
    boolean updateRecord(DataRecord record) throws RemoteException;
    boolean deleteRecord(String id) throws RemoteException;
    String getServerId() throws RemoteException;
    ServerInfo getServerInfo() throws RemoteException;
}
