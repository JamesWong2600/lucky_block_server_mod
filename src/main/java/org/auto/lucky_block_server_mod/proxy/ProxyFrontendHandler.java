package org.auto.lucky_block_server_mod.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;

import java.net.InetSocketAddress;

public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {

    private volatile Channel outboundChannel;
    private final GameProxyServer proxyServer;
    public ProxyFrontendHandler(GameProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel inboundChannel = ctx.channel();

        // 1. 取得玩家的識別碼 (這裡暫時用 IP，若你有 UUID 傳遞邏輯可替換)
        String playerIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

        // 2. 向 GameProxyServer 請求路由 (自動處理快取與 Redis 查詢)
        String backendAddress = proxyServer.getBestBackend(playerIp);

        if (backendAddress == null) {
            inboundChannel.close();
            return;
        }

        // 3. 解析地址
        String[] parts = backendAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        // 4. 連線邏輯
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(inboundChannel.getClass())
                .handler(new ProxyBackendHandler(inboundChannel))
                .option(ChannelOption.AUTO_READ, false);

        b.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                outboundChannel = future.channel();
                inboundChannel.read();
            } else {
                inboundChannel.close();
            }
        });
    }
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        if (outboundChannel.isActive()) {
            // 將玩家發送的資料，直接轉發給後端伺服器
            outboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(io.netty.buffer.Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
