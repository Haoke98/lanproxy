package org.fengfei.lanproxy.server.utils;

import io.netty.buffer.ByteBuf;
import org.fengfei.lanproxy.server.requestlogs.TcpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PacketBuffer {
    private static final Logger logger = LoggerFactory.getLogger(PacketBuffer.class);
    private final Map<String, ByteBuf> bufferMap = new HashMap<>();
    private final Map<String, Long> lastUpdateTime = new HashMap<>();
    private static final long TIMEOUT = 30000; // 30秒超时

    private static class PacketMetadata {
        ByteBuf data;
        long lastUpdateTime;
        long sequenceNumber;
        long expectedNextSeq;

        PacketMetadata(ByteBuf data, long seq) {
            this.data = data;
            this.lastUpdateTime = System.currentTimeMillis();
            this.sequenceNumber = seq;
            this.expectedNextSeq = seq + data.readableBytes();
        }
    }

    private final Map<String, PacketMetadata> packetMap = new HashMap<>();

    public void addPacket(TcpPacket packet) {
        synchronized (packetMap) {
            PacketMetadata existing = packetMap.get(packet.getUserId());
            if (existing == null) {
                packetMap.put(packet.getUserId(), new PacketMetadata(packet.getData().copy(), packet.getSequenceNumber()));
            } else if (packet.getSequenceNumber() == existing.expectedNextSeq) {
                // 连续的包，追加数据
                ByteBuf newBuf = existing.data.copy();
                newBuf.writeBytes(packet.getData());
                existing.data.release();
                existing.data = newBuf;
                existing.expectedNextSeq = packet.getSequenceNumber() + packet.getData().readableBytes();
                existing.lastUpdateTime = System.currentTimeMillis();
            } else if (packet.getSequenceNumber() > existing.expectedNextSeq) {
                // 包丢失，记录日志
                logger.warn("Packet loss detected for key {}, expected seq {}, got {}",
                        packet.getUserId(), existing.expectedNextSeq, packet.getSequenceNumber());
            }
        }
    }

    public ByteBuf getCompletePacket(String key) {
        synchronized (bufferMap) {
            ByteBuf buf = bufferMap.get(key);
            if (buf != null) {
                // 检查是否是完整的包
                if (isCompletePacket(buf)) {
                    bufferMap.remove(key);
                    lastUpdateTime.remove(key);
                    return buf;
                }
            }
            return null;
        }
    }

    private boolean isCompletePacket(ByteBuf buf) {
        // 根据不同协议判断包是否完整
        if (PacketParser.isHttpRequest(buf)) {
            String content = buf.toString(StandardCharsets.UTF_8);
            // 检查是否包含完整的HTTP头
            if (content.contains("\r\n\r\n")) {
                // 如果有Content-Length，检查body是否完整
                int headerEnd = content.indexOf("\r\n\r\n");
                String headers = content.substring(0, headerEnd);
                if (headers.contains("Content-Length: ")) {
                    int contentLength = getContentLength(headers);
                    return buf.readableBytes() >= headerEnd + 4 + contentLength;
                }
                return true;
            }
            return false;
        }
        // 其他协议的完整性检查...
        return true;
    }

    private int getContentLength(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.startsWith("Content-Length: ")) {
                return Integer.parseInt(line.substring("Content-Length: ".length()).trim());
            }
        }
        return 0;
    }

    public void cleanup() {
        synchronized (bufferMap) {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<String, Long>> it = lastUpdateTime.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                if (now - entry.getValue() > TIMEOUT) {
                    String key = entry.getKey();
                    ByteBuf buf = bufferMap.remove(key);
                    if (buf != null) {
                        buf.release();
                    }
                    it.remove();
                }
            }
        }
    }
} 