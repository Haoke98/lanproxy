package org.fengfei.lanproxy.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalServiceHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(LocalServiceHandler.class);
    private final Channel proxyChannel;
    private final String serialNumber;

    public LocalServiceHandler(Channel proxyChannel, String serialNumber) {
        this.proxyChannel = proxyChannel;
        this.serialNumber = serialNumber;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);
        
        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setType(ProxyMessage.P_TYPE_TRANSFER);
        proxyMessage.setSerialNumber(Long.parseLong(serialNumber));
        proxyMessage.setData(data);
        
        proxyChannel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
        proxyMessage.setSerialNumber(Long.parseLong(serialNumber));
        proxyChannel.writeAndFlush(proxyMessage);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error in local service connection", cause);
        ctx.close();
    }
} 