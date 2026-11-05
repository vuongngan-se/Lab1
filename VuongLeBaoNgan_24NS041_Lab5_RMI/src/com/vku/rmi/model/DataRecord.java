package com.vku.rmi.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;              // Mã sinh viên hoặc ID bản ghi (vd: "24NS001")
    private String name;            // Họ và tên
    private String department;      // Khoa / Ngành (vd: "Khoa Học Máy Tính")
    private double score;           // Điểm số / GPA (vd: 3.85)
    private long version;           // Phiên bản dữ liệu (để phát hiện conflict)
    private long lastModified;      // Thời gian sửa đổi cuối
    private String updatedByServer; // Server thực hiện thay đổi ("SERVER_1" hoặc "SERVER_2")

    public DataRecord() {
        this.lastModified = System.currentTimeMillis();
        this.version = 1;
    }

    public DataRecord(String id, String name, String department, double score, String serverId) {
        this();
        this.id = id;
        this.name = name;
        this.department = department;
        this.score = score;
        this.updatedByServer = serverId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public String getUpdatedByServer() { return updatedByServer; }
    public void setUpdatedByServer(String updatedByServer) { this.updatedByServer = updatedByServer; }

    public String getFormattedDate() {
        return new SimpleDateFormat("HH:mm:ss dd/MM").format(new Date(lastModified));
    }

    public void incrementVersion(String serverId) {
        this.version++;
        this.lastModified = System.currentTimeMillis();
        this.updatedByServer = serverId;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (Điểm: %.2f) [v%d bởi %s]",
                id, name, department, score, version, updatedByServer);
    }
}
