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
                            // 這裡動態路由
                            String backendAddress = getBestBackend();
                            if (backendAddress != null) {
                                String[] parts = backendAddress.split(":");
                                String host = parts[0];
                                int port = Integer.parseInt(parts[1]);
                                // 初始化 ProxyFrontendHandler 進行轉發
                                ch.pipeline().addLast(new ProxyFrontendHandler(host, port));
                            } else {
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

    // 💡 核心路由演算法：從 Redis 讀取在線人數，選人數最少的後端
    private String getBestBackend(UUID playerUuid) {
        // --- 第一層：檢查本地記憶體快取 (Local Cache) ---

        PlayerData data = DataMap.getPlayerData(playerUuid);


        if (playerUuid != null && localSessionCache.containsKey(playerUuid)) {
            String cachedServer = localSessionCache.get(playerUuid);
            // 若該後端依然在 Redis 中，則直接回傳
            if (isBackendAvailable(cachedServer)) {
                return cachedServer;
            }
        }

        // --- 第二層：檢查 Redis 會話記錄 (Session Affinity) ---
        try (Jedis jedis = jedisPool.getResource()) {
            if (playerUuid != null) {
                String lastServer = jedis.get("player:session:" + playerUuid);
                if (lastServer != null && isBackendAvailable(lastServer)) {
                    localSessionCache.put(playerUuid, lastServer); // 更新本地快取
                    return lastServer;
                }
            }

            // --- 第三層：負載均衡 (Least Connections) ---
            Map<String, String> servers = jedis.hgetAll("game:servers");
            if (servers == null || servers.isEmpty()) return null;

            String bestServer = null;
            int minPlayers = Integer.MAX_VALUE;

            for (Map.Entry<String, String> entry : servers.entrySet()) {
                try {
                    int playerCount = Integer.parseInt(entry.getValue());
                    if (playerCount < minPlayers) {
                        minPlayers = playerCount;
                        bestServer = entry.getKey();
                    }
                } catch (NumberFormatException ignored) {}
            }

            // 記錄路由結果並回寫 Redis 與 本地快取
            if (bestServer != null) {
                if (playerUuid != null) {
                    localSessionCache.put(playerUuid, bestServer);
                    jedis.setex("player:session:" + playerUuid, 1800, bestServer); // 設定 30 分鐘有效期
                }
            }

            System.out.println("路由決策 -> 選擇後端: " + bestServer + " (人數: " + minPlayers + ")");
            return bestServer;
        } catch (Exception e) {
            e.printStackTrace();
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