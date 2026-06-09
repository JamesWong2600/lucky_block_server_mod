package org.auto.lucky_block_server_mod.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.minecraft.server.MinecraftServer;
import org.auto.lucky_block_server_mod.cache.DataMap;
import org.auto.lucky_block_server_mod.cache.PlayerData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.CONFIG;

public class GameProxyServer {

    private final int proxyPort; // 更改名稱，使其更明確
    private final JedisPool jedisPool;

    public GameProxyServer(int proxyPort, String redisHost, int redisPort) {
        this.proxyPort = proxyPort;
        this.jedisPool = new JedisPool(redisHost, redisPort);
    }

    public static void startProxyServer(MinecraftServer server) {
        String redisHost = CONFIG.redis.host;
        int redisPort = CONFIG.redis.port;
        int proxyPort = server.getServerPort()+100; // 設定你想開放的代理端口

        new Thread(() -> {
            try {
                // 這裡直接 new 建構子
                GameProxyServer proxy = new GameProxyServer(proxyPort, redisHost, redisPort);
                System.out.println("🚀 代理伺服器已啟動，監聽 Port: " + proxyPort);
                proxy.start();
            } catch (Exception e) {
                System.err.println("❌ 代理伺服器啟動失敗");
                e.printStackTrace();
            }
        }, "Proxy-Server-Thread").start();
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 1. 取得玩家的連線來源 IP
                            String clientIp = ch.remoteAddress().getAddress().getHostAddress();

                            // 2. 呼叫我們剛剛改寫的、帶有快取與路由邏輯的方法
                            String backendAddress = getBestBackend(clientIp);

                            if (backendAddress != null) {
                                String[] parts = backendAddress.split(":");
                                String host = parts[0];
                                int port = Integer.parseInt(parts[1]);

                                // 3. 使用 ProxyFrontendHandler 進行透明轉發
                                ch.pipeline().addLast(new ProxyFrontendHandler(GameProxyServer.this));
                            } else {
                                // 沒有可用的後端，直接斷開
                                ch.close();
                            }
                        }
                    })
                    .childOption(ChannelOption.AUTO_READ, false);

            // ⚠️ 關鍵修正：這裡綁定你傳入的 proxyPort
            ChannelFuture f = b.bind(this.proxyPort).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            jedisPool.close();
        }
    }

    
//
//    public void start() throws Exception {
//        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
//        EventLoopGroup workerGroup = new NioEventLoopGroup();
//
//        try {
//            ServerBootstrap b = new ServerBootstrap();
//            b.group(bossGroup, workerGroup)
//                    .channel(NioServerSocketChannel.class)
//                    .childHandler(new ChannelInitializer<SocketChannel>() {
//                        @Override
//                        protected void initChannel(SocketChannel ch) {
//                            // 1. 玩家連線進來時，動態向 Redis 獲取最佳後端 IP:Port
//                            String backendAddress = getBestBackend();
//                            if (backendAddress == null) {
//                                System.err.println("無可用後端伺服器，拒絕連線。");
//                                ch.close();
//                                return;
//                            }
//
//                            String[] parts = backendAddress.split(":");
//                            String backendHost = parts[0];
//                            int backendPort = Integer.parseInt(parts[1]);
//
//                            // 2. 配置 雙向轉發 Handler
//                            ch.pipeline().addLast(new ProxyFrontendHandler(backendHost, backendPort));
//                        }
//                    })
//                    .childOption(ChannelOption.AUTO_READ, false); // 控制流速，防止 OOM
//
//            System.out.println("Proxy 伺服器已啟動，監聽連接埠: " + localPort);
//            ChannelFuture f = b.bind(localPort).sync();
//            f.channel().closeFuture().sync();
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//            jedisPool.close();
//        }
//    }
    private final Map<String, String> localSessionCache = new ConcurrentHashMap<>();
    // 💡 核心路由演算法：從 Redis 讀取在線人數，選人數最少的後端
    public String getBestBackend(String playerIdentifier) {
        // 優先檢查本地記憶體
        if (localSessionCache.containsKey(playerIdentifier)) {
            String cached = localSessionCache.get(playerIdentifier);
            if (isBackendAvailable(cached)) return cached;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // 檢查 Redis 中的會話記錄
            String lastServer = jedis.get("player:session:" + playerIdentifier);
            if (lastServer != null && isBackendAvailable(lastServer)) {
                localSessionCache.put(playerIdentifier, lastServer);
                return lastServer;
            }

            // 若無記錄，執行負載均衡
            Map<String, String> servers = jedis.hgetAll("game:servers");
            if (servers == null || servers.isEmpty()) return null;

            String bestServer = null;
            int minPlayers = Integer.MAX_VALUE;
            for (Map.Entry<String, String> entry : servers.entrySet()) {
                try {
                    int count = Integer.parseInt(entry.getValue());
                    if (count < minPlayers) {
                        minPlayers = count;
                        bestServer = entry.getKey();
                    }
                } catch (Exception ignored) {}
            }

            // 記錄路由結果
            if (bestServer != null) {
                localSessionCache.put(playerIdentifier, bestServer);
                jedis.setex("player:session:" + playerIdentifier, 1800, bestServer);
            }
            return bestServer;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isBackendAvailable(String backend) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hexists("game:servers", backend);
        } catch (Exception e) {
            return false;
        }
    }

//    public static void main(String[] args) throws Exception {
//        // 監聽本地 8888 埠，Redis 連接本地 6379
//        new GameProxyServer("192.168.1.102", 6379, sever).start();
//    }
}