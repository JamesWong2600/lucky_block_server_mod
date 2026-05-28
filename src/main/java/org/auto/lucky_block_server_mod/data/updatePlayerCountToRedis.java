package org.auto.lucky_block_server_mod.data;

import net.minecraft.server.MinecraftServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.auto.lucky_block_server_mod.data.player_amount.getLocalPlayerAmount;
import static org.auto.lucky_block_server_mod.data.player_amount.pool;

public class updatePlayerCountToRedis {

    // 設定你的內網 IP 和連接埠，建議從 config.yml 讀取



    public static String getServerIdentifier(MinecraftServer server) {
        try {
            // 從 server.properties 獲取設定的 Port
            int port = server.getServerPort();

            // 獲取本機內網 IP
            String ip = InetAddress.getLocalHost().getHostAddress();

            return ip + ":" + port;
        } catch (UnknownHostException e) {
            return "unknown:0";
        }
    }


    public static void updatePlayerCount(MinecraftServer server) {
        int count = getLocalPlayerAmount(server); // 傳入你的 MinecraftServer 對象

        try (Jedis jedis = pool.getResource()) {
            // 將人數寫入 Redis Hash
            jedis.hset("game:servers", getServerIdentifier(server), String.valueOf(count));
        } catch (Exception e) {
            System.out.println("無法連接 Redis: " + e.getMessage());
        }
    }


}
