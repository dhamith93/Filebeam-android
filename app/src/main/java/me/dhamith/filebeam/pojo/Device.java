package me.dhamith.filebeam.pojo;

public class Device {
    private String host;
    private String key;

    public Device(String host, String key) {
        this.host = host;
        this.key = key;
    }

    public String getIp() {
        return host;
    }

    public void setIp(String ip) {
        this.host = ip;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
