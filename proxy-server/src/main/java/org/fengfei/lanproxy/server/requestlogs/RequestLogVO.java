package org.fengfei.lanproxy.server.requestlogs;

import java.text.SimpleDateFormat;

// 添加日志VO类用于页面展示
public class RequestLogVO {

    private String ip;
    private int port;
    private String time;
    private String protocol;
    private long packetSize;
    private String requestInfo;

    public RequestLogVO(RequestLog log) {
        this.ip = log.getIp();
        this.port = log.getPort();
        this.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log.getTime());
        this.requestInfo = log.getRequestInfo();
        this.protocol = log.getProtocol();
        this.packetSize = log.getPacketSize();
    }

    // getter方法
    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getTime() {
        return time;
    }

    public String getRequestInfo() {
        return requestInfo;
    }

    public String getProtocol() {
        return protocol;
    }

    public long getPacketSize() {
        return packetSize;
    }

}
