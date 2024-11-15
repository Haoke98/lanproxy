package org.fengfei.lanproxy.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.fengfei.lanproxy.server.requestlogs.RequestLog;
import org.fengfei.lanproxy.server.requestlogs.RequestLogCollector;
import org.fengfei.lanproxy.server.utils.PacketParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestLogHandler {
    private static final Logger logger = LoggerFactory.getLogger(RequestLogHandler.class);

    // 使用队列异步处理日志
    private final BlockingQueue<RequestLog> logQueue = new LinkedBlockingQueue<>();

    public void logRequest(Channel channel, String userId, ByteBuf data) {
        RequestLog log = new RequestLog();
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        log.setIp(address.getAddress().getHostAddress());
        log.setPort(address.getPort());
        log.setTime(new Date());
        log.setUserId(userId);
        logger.info("buf.readableBytes(): {}", data.readableBytes());
        log.setData(data);

        // 异步写入日志队列
        logQueue.offer(log);
    }

    // 日志处理线程
    public class LogProcessor implements Runnable {
        public void run() {
            while (true) {
                try {
                    RequestLog log = logQueue.take();
                    PacketParser.PacketInfo packetInfo = PacketParser.parsePacket(log.getData());
                    log.setRequestInfo(packetInfo.getDetailedInfo());
                    log.setProtocol(packetInfo.getProtocol());
                    log.setPacketSize(log.getData().readableBytes());
                    
                    logger.info("Request from {}:{} - {} ({} bytes)", 
                        log.getIp(), log.getPort(), packetInfo.getProtocol(), log.getPacketSize());
                    saveLog(log);
                } catch (Exception e) {
                    logger.error("Error processing log", e);
                }
            }
        }
    }

    // 修改日志处理逻辑，保存最近的日志
    private void saveLog(RequestLog log) {
        RequestLogCollector.saveLog(log);
    }



}
