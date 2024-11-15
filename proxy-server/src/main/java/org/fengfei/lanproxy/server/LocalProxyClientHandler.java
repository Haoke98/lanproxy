package org.fengfei.lanproxy.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fengfei.lanproxy.protocol.ProxyMessage.C_TYPE_AUTH;

public class LocalProxyClientHandler extends SimpleChannelInboundHandler<ProxyMessage> {
    private static final Logger logger = LoggerFactory.getLogger(LocalProxyClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws Exception {
        logger.debug("Local proxy client received message: {}", msg.getType());
        
        switch (msg.getType()) {
            case C_TYPE_AUTH:  // TYPE_AUTH_RESULT
                handleAuthResult(ctx, msg);
                break;
            case ProxyMessage.TYPE_CONNECT:
                handleConnect(ctx, msg);
                break;
            case ProxyMessage.TYPE_DISCONNECT:
                handleDisconnect(ctx, msg);
                break;
            default:
                logger.warn("Unknown message type: {}", msg.getType());
        }
    }

    private void handleAuthResult(ChannelHandlerContext ctx, ProxyMessage msg) {
        if (LocalProxyClient.LOCAL_CLIENT_KEY.equals(msg.getUri())) {
            logger.info("Local proxy client auth success");
        } else {
            logger.error("Local proxy client auth failed");
            ctx.close();
        }
    }

    private void handleConnect(ChannelHandlerContext ctx, ProxyMessage msg) {
        // 处理连接请求
        String lanInfo = msg.getUri();
        String[] serverInfo = lanInfo.split(":");
        String host = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);
        
        // 连接到本地服务
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new LocalServiceHandler(ctx.channel(), String.valueOf(msg.getSerialNumber())));
                    }
                });
        
        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.debug("Connected to local service {}:{}", host, port);
            } else {
                logger.error("Failed to connect to local service {}:{}", host, port);
                ProxyMessage result = new ProxyMessage();
                result.setType(ProxyMessage.TYPE_DISCONNECT);
                result.setSerialNumber(msg.getSerialNumber());
                ctx.writeAndFlush(result);
            }
        });
    }

    private void handleDisconnect(ChannelHandlerContext ctx, ProxyMessage msg) {
        // 处理断开连接请求
        logger.debug("Received disconnect message for serial {}", msg.getSerialNumber());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Local proxy client disconnected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Local proxy client error", cause);
        ctx.close();
    }
} 