package org.fengfei.lanproxy.server.requestlogs;

import java.text.SimpleDateFormat;
import io.netty.buffer.ByteBuf;

// 添加日志VO类用于页面展示
public class RequestLogVO {

    private String ip;
    private int port;
    private String time;
    private String protocol;
    private long packetSize;
    private String requestInfo;
    private String hexContent;
    private String asciiContent;

    public RequestLogVO(TcpPacket pkt) {
        this.ip = pkt.getSourceIp();
        this.port = pkt.getSourcePort();
        this.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(pkt.getTime());
        this.requestInfo = pkt.getRequestInfo();
        this.protocol = pkt.getProtocol();
        this.packetSize = pkt.getPacketSize();
        ByteBuf buf = pkt.getData();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        this.setPacketData(bytes);
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

    public String getHexContent() {
        return hexContent;
    }

    public void setHexContent(String hexContent) {
        this.hexContent = hexContent;
    }

    public String getAsciiContent() {
        return asciiContent;
    }

    public void setAsciiContent(String asciiContent) {
        this.asciiContent = asciiContent;
    }

    public void setPacketData(byte[] data) {
        StringBuilder hexBuilder = new StringBuilder();
        StringBuilder asciiBuilder = new StringBuilder();

        for (byte b : data) {
            hexBuilder.append(String.format("%02X ", b));

            if (b >= 32 && b < 127) {
                asciiBuilder.append((char) b);
            } else {
                asciiBuilder.append('.');
            }
        }

        this.hexContent = hexBuilder.toString();
        this.asciiContent = asciiBuilder.toString();
    }
}
