package me.dhamith.filebeam.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Transfer {
    public static final String PENDING = "pending";
    public static final String CANCELED = "canceled";
    public static final String STARTED = "started";
    public static final String COMPLETED = "completed";
    public static final String ERROR = "error";
    public static final String DOWNLOAD = "download";
    public static final String UPLOAD = "upload";

    private int id;
    private String host;
    private int port;
    private int filePort;
    private long completedBytes;
    private long startTime;
    private long endTime;
//    private int progress;
    private String status;
    private String type;
    private File file;
    private Exception error;
    private static List<Transfer> transfers;

    public Transfer(int id, String host, int port, int filePort, String status, String type, File file) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.filePort = filePort;
        this.status = status;
        this.type = type;
        this.file = file;
        this.completedBytes = 0;
        this.startTime = -1;
        this.endTime = -1;
    }

    public static List<Transfer> getTransfers() {
        if (transfers == null) {
            transfers = new ArrayList<>();
        }
        return transfers;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Transfer other = (Transfer) obj;
        return Objects.equals(file.getName(), other.file.getName()) && Objects.equals(file.getSize(), other.file.getSize());
    }

    public boolean isActive() {
        return this.status.equals(STARTED);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getFilePort() {
        return filePort;
    }

    public void setFilePort(int filePort) {
        this.filePort = filePort;
    }

    public long getCompletedBytes() {
        return completedBytes;
    }

    public void setCompletedBytes(long completedBytes) {
        this.completedBytes = completedBytes;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }
}
