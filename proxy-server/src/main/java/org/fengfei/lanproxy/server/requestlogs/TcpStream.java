package org.fengfei.lanproxy.server.requestlogs;

import org.fengfei.lanproxy.server.utils.PacketBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TcpStream {
    private static final Logger logger = LoggerFactory.getLogger(TcpStream.class);
    private String sourceIp;
    private int sourcePort;
    private String destIp;
    private int destPort;
    private long sequenceNumber; // 当前期望的序列号
    private ByteArrayOutputStream buffer; // 用来存储数据的缓冲区

    // 构造函数和其他管理方法
    public TcpStream(String sourceIp, int sourcePort, String destIp, int destPort) {
        this.sourceIp = sourceIp;
        this.sourcePort = sourcePort;
        this.destIp = destIp;
        this.destPort = destPort;
        this.sequenceNumber = 0;
        this.buffer = new ByteArrayOutputStream();
    }

    // 更新流数据并返回是否成功
    public boolean appendData(byte[] data, long seqNumber) {
        if (seqNumber == sequenceNumber) {
            // 如果序列号是期望的，直接添加数据
            try {
                buffer.write(data);
                sequenceNumber += data.length;
                return true;
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        } else {
            // 如果序列号不匹配，可以选择缓存数据或者重新排序
            System.out.println("Unexpected sequence number: " + seqNumber);
        }
        return false;
    }

    // 获取已重组的完整数据
    public byte[] getData() {
        return buffer.toByteArray();
    }
}

