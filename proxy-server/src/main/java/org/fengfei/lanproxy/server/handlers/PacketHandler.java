package org.fengfei.lanproxy.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.fengfei.lanproxy.server.requestlogs.TcpStream;
import org.fengfei.lanproxy.server.requestlogs.TcpPacket;
import org.fengfei.lanproxy.server.requestlogs.PacketCollector;
import org.fengfei.lanproxy.server.requestlogs.RequestLogVO;
import org.fengfei.lanproxy.server.utils.PacketBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(PacketHandler.class);

    // 使用队列异步处理日志
    private final BlockingQueue<TcpPacket> packetQueue = new LinkedBlockingQueue<>();

    private final PacketBuffer packetBuffer = new PacketBuffer();
    private static final Map<String, TcpStream> streams = new HashMap<>();

    public void logRequest(Channel channel, String userId, ByteBuf data) {
        TcpPacket packet = new TcpPacket();
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress(); // 从外部访问的用户的IP:Port
        InetSocketAddress localAddress = (InetSocketAddress) channel.localAddress(); // 从外部访问的用户访问的目标Host:Port

        packet.setSourceIp(address.getAddress().getHostAddress());
        packet.setSourcePort(address.getPort());

        packet.setDestIp(localAddress.getAddress().getHostAddress());
        packet.setDestPort(localAddress.getPort());

        packet.setUserId(userId);
        logger.info("buf.readableBytes(): {}", data.readableBytes());
        packet.setData(data);
        // 异步写入日志队列
        packetQueue.offer(packet);
    }

    // 日志处理线程
    public class LogProcessor implements Runnable {
        public void run() {
            while (true) {
                try {
                    TcpPacket packet = packetQueue.take();
                    logger.info("Request from {}:{} - {} ({} bytes)", packet.getSourceIp(), packet.getSourcePort(), packet.getProtocol(), packet.getPacketSize());
                    saveLog(packet);

                    processPacket(packet);
//                    // 添加到缓冲区
//                    packetBuffer.addPacket(packet);
//                    // 尝试获取完整的包
//                    ByteBuf completePkt = packetBuffer.getCompletePacket(packet.getUserId());
//                    if (completePkt != null) {
//                        // 处理完整的包
//                        TcpPacket completeTcpUdpPkt = new TcpPacket();
//                        completeTcpUdpPkt.setData(completePkt);
//                        RequestLogVO vo = new RequestLogVO(completeTcpUdpPkt);
//                        logger.info(vo.getHexContent());
//                    }


                } catch (Exception e) {
                    logger.error("Error processing log", e);
                }
            }
        }
    }

    // 修改日志处理逻辑，保存最近的日志
    private void saveLog(TcpPacket log) {
        PacketCollector.saveLog(log);
    }

    public void processPacket(TcpPacket packet) {
        String streamKey = packet.getSourceIp() + ":" + packet.getSourcePort() + "-" +
                packet.getDestIp() + ":" + packet.getDestPort();

        TcpStream stream = streams.get(streamKey);
        if (stream == null) {
            // 创建新的流
            stream = new TcpStream(packet.getSourceIp(), packet.getSourcePort(),
                    packet.getDestIp(), packet.getDestPort());
            streams.put(streamKey, stream);
        }

        // 将数据包中的负载添加到对应流
        if (stream.appendData(packet.getPayload(), packet.getSequenceNumber())) {
            System.out.println("Data appended successfully.");
        } else {
            System.out.println("Data not appended. Sequence number out of order.");
        }
    }

    public byte[] getReassembledData(String sourceIp, int sourcePort, String destIp, int destPort) {
        String streamKey = sourceIp + ":" + sourcePort + "-" + destIp + ":" + destPort;
        TcpStream stream = streams.get(streamKey);
        return stream != null ? stream.getData() : null;
    }


}
