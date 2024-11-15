package org.fengfei.lanproxy.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.fengfei.lanproxy.server.config.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalProxyClient {
    private static final Logger logger = LoggerFactory.getLogger(LocalProxyClient.class);
    public static final String LOCAL_CLIENT_KEY = "local_proxy_client";
    private EventLoopGroup workerGroup;
    private Channel channel;

    public void init() {
        workerGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LocalProxyClientHandler());
                    }
                });

        try {
            // 连接到本地回环地址
            ChannelFuture future = bootstrap.connect("127.0.0.1", 4900).sync();
            channel = future.channel();
            
            // 发送注册消息
            ProxyMessage message = new ProxyMessage();
            message.setType(ProxyMessage.C_TYPE_AUTH);
            message.setUri(LOCAL_CLIENT_KEY);
            channel.writeAndFlush(message);
            
            logger.info("Local proxy client started");
            ProxyConfig.getInstance().addLocalProxyMapping();
        } catch (Exception e) {
            logger.error("Failed to start local proxy client", e);
        }
    }

    public void stop() {
        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
} 