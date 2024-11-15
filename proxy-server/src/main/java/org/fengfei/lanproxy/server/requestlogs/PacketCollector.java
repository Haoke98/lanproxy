package org.fengfei.lanproxy.server.requestlogs;

import java.util.ArrayList;
import java.util.List;

public class PacketCollector {
    private static final List<TcpPacket> recentPackets = new ArrayList<>();
    // 存储最近的日志记录
    private static final int MAX_LOG_SIZE = 1000;

    // 添加获取日志的方法
    public static List<RequestLogVO> getRecentPackets() {
        List<RequestLogVO> logs = new ArrayList<>();
        synchronized (recentPackets) {
            for (TcpPacket log : recentPackets) {
                logs.add(new RequestLogVO(log));
            }
        }
        return logs;
    }

    // 修改日志处理逻辑，保存最近的日志
    public static void saveLog(TcpPacket log) {
        synchronized (recentPackets) {
            if (recentPackets.size() >= MAX_LOG_SIZE) {
                recentPackets.remove(0);
            }
            recentPackets.add(log);
        }
    }
}
