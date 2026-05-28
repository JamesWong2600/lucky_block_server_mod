package org.auto.lucky_block_server_mod.data;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.auto.lucky_block_server_mod.cache.DataMap;
import org.auto.lucky_block_server_mod.config.ModConfig;
import org.auto.lucky_block_server_mod.scoreboard.EventScoreboard;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.auto.lucky_block_server_mod.lobby.player_join_teleport_lobby.isClonePlayer;
import static org.auto.lucky_block_server_mod.message.cross_server_msg.startListening;


public class player_amount {

    public static JedisPool pool;
    private static ModConfig config;
    private static final String REDIS_KEY_PREFIX = "server:players:";

    public static void init(Path configPath, DataMap dataManager, MinecraftServer server) {
        File configFile = configPath.resolve("lucky_block_config.toml").toFile();

        try {
            // 1. 讀取或建立預設 TOML
            if (!configFile.exists()) {
                config = new ModConfig();
                TomlWriter writer = new TomlWriter();
                writer.write(config, configFile); // 自動生成預設配置文件
                System.out.println("Default config created at: " + configFile.getPath());
            } else {
                config = new Toml().read(configFile).to(ModConfig.class);
            }

            // 2. 初始化 MongoDB (從 TOML 讀取資料)
            dataManager.initMongo(
                    config.database.uri,
                    config.database.db_name,
                    config.database.collection_name,
                    config.database.server_data_collection
            );
            System.out.println("MongoDB initialized from TOML config.");

            // 3. 初始化 Redis 連接池
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(8);
// --- 新增以下設定 ---
            poolConfig.setTestOnBorrow(true);       // 每次拿連線時都先 Ping 一下 (確保沒斷)
            poolConfig.setTestWhileIdle(true);      // 閒置時也進行檢測
            poolConfig.setMinEvictableIdleTimeMillis(60000); // 連線至少閒置多久會被清除 (1分鐘)
            poolConfig.setTimeBetweenEvictionRunsMillis(30000); // 檢測執行頻率

            pool = new JedisPool(
                    poolConfig,
                    config.redis.host,
                    config.redis.port,
                    2000, // Connection Timeout
                    (config.redis.password == null || config.redis.password.isEmpty()) ? null : config.redis.password
            );
            System.out.println("Redis initialized for server: " + config.server.id);
            startListening(server);

        } catch (Exception e) {
            System.err.println("Failed to load config or initialize databases!");
            e.printStackTrace();
        }
    }

    // --- Redis 人數同步功能 ---

    public static void updateRedisCount(MinecraftServer server) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            int count = server.getCurrentPlayerCount();
            jedis.setex(REDIS_KEY_PREFIX + config.server.id, config.server.expire_seconds, String.valueOf(count));
        } catch (Exception e) {
            System.err.println("Redis update error: " + e.getMessage());
        }
    }

    public static int getGlobalPlayerAmount() {
        if (pool == null) return 0;

        int total = 0;
        // 使用 hvals 直接獲取所有 Field 的值，不需要遍歷 Key
        try (Jedis jedis = pool.getResource()) {
            // 直接取得 "game:servers" 這個 Hash 下的所有人數值
            Map<String, String> allServers = jedis.hgetAll("game:servers");

            if (allServers != null) {
                for (String count : allServers.values()) {
                    try {
                        total += Integer.parseInt(count);
                    } catch (NumberFormatException e) {
                        // 忽略格式錯誤的數值
                    }
                }
            }
        } catch (Exception e) {
            // 使用你的日誌系統記錄錯誤，而不是 e.printStackTrace()
            System.err.println("無法獲取全局人數: " + e.getMessage());
        }
        return total;
    }

    public static ModConfig getConfig() {
        return config;
    }

    public static int getLocalPlayerAmount(MinecraftServer server) {
        if (server == null) return 0;
        return (int) server.getPlayerManager().getPlayerList().stream()
                .filter(player -> !isClonePlayer(player))
                .count();
    }


}