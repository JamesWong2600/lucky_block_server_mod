package org.auto.lucky_block_server_mod.data;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.auto.lucky_block_server_mod.scoreboard.EventScoreboard;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Set;

public class player_amount {
    private static final JedisPool pool = new JedisPool("localhost", 6379);
    public static final String SERVER_ID = "server-01"; // 每個子服設定不同的 ID

    /**
     * 獲取本地伺服器玩家數量
     */
    public static int getLocalPlayerAmount(MinecraftServer server) {
        return server.getCurrentPlayerCount();
    }

    /**
     * 獲取全域 (Redis) 玩家數量
     */
    public static int getGlobalPlayerAmount() {
        int total = 0;
        try (Jedis jedis = pool.getResource()) {
            // 獲取所有記錄人數的 Key
            Set<String> keys = jedis.keys("server:players:*");
            for (String key : keys) {
                String val = jedis.get(key);
                if (val != null) {
                    total += Integer.parseInt(val);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    /**
     * 更新當前伺服器人數到 Redis (需每秒呼叫一次)
     */
    public static void updateRedisCount(MinecraftServer server) {
        try (Jedis jedis = pool.getResource()) {
            int count = getLocalPlayerAmount(server);
            // 設定 5 秒過期，防止伺服器崩潰後人數殘留
            jedis.setex("server:players:" + SERVER_ID, 5, String.valueOf(count));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
