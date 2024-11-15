package org.fengfei.lanproxy.server.requestlogs;

import io.netty.buffer.ByteBuf;
import org.fengfei.lanproxy.server.utils.PacketParser;

import java.util.Date;

//TCP/UDP请求包
public class TcpPacket {
    private String sourceIp;
    private int sourcePort;
    private Date time;
    private String destIp;
    private int destPort;
    private String userId;
    private long sequenceNumber;
    private long acknowledgmentNumber;
    private ByteBuf data;
    private String protocol;
    private long packetSize;
    private String requestInfo;

    public TcpPacket() {
        this.time = new Date();
    }

    public TcpPacket(ByteBuf buf) {
        this();
        this.setData(buf);
    }

    public TcpPacket(String sourceIp, int sourcePort, String destIp, int destPort, ByteBuf payload) {
        this(payload);
        this.setSourceIp(sourceIp);
        this.setSourcePort(sourcePort);
        this.setDestIp(destIp);
        this.setDestPort(destPort);
    }

    public void setSourceIp(String ip) {
        this.sourceIp = ip;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public void setRequestInfo(String requestInfo) {
        this.requestInfo = requestInfo;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public String getRequestInfo() {
        return requestInfo;
    }

    public String getUserId() {
        return userId;
    }

    public Date getTime() {
        return time;
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
        PacketParser.PacketInfo packetInfo = PacketParser.parsePacket(data);
        this.protocol = packetInfo.getProtocol();
        this.requestInfo = packetInfo.getDetailedInfo();
        this.packetSize = data.readableBytes();
    }

    public String getProtocol() {
        return protocol;
    }

    public long getPacketSize() {
        return packetSize;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public long getAcknowledgmentNumber() {
        return acknowledgmentNumber;
    }

    public String getDestIp() {
        return destIp;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public void setDestIp(String destIp) {
        this.destIp = destIp;
    }

    public byte[] getPayload() {
        byte[] bytes = new byte[this.data.readableBytes()];
        this.data.getBytes(this.data.readerIndex(), bytes);
        return bytes;
    }
}