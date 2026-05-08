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
import java.util.Set;

public class player_amount {

    private static JedisPool pool;
    private static ModConfig config;
    private static final String REDIS_KEY_PREFIX = "server:players:";

    public static void init(Path configPath, DataMap dataManager) {
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

            pool = new JedisPool(
                    poolConfig,
                    config.redis.host,
                    config.redis.port,
                    2000, // Timeout
                    (config.redis.password == null || config.redis.password.isEmpty()) ? null : config.redis.password
            );
            System.out.println("Redis initialized for server: " + config.server.id);

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
        try (Jedis jedis = pool.getResource()) {
            Set<String> keys = jedis.keys(REDIS_KEY_PREFIX + "*");
            for (String key : keys) {
                String val = jedis.get(key);
                if (val != null) total += Integer.parseInt(val);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    public static ModConfig getConfig() {
        return config;
    }

    public static int getLocalPlayerAmount(MinecraftServer server) {
        if (server == null) return 0;
        return server.getCurrentPlayerCount();
    }
}