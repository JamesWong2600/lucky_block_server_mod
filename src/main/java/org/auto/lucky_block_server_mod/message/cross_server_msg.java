package org.auto.lucky_block_server_mod.message;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil.GSON;
import static org.auto.lucky_block_server_mod.data.player_amount.pool;

public class cross_server_msg {

    private static final String CHANNEL = "minecraft:player_chat";
    private static Thread subscribeThread;
    public static void publishEvent(UUID playerUuid, String playerName, String message) {
        if (pool == null) return;

        // 異步執行，避免 Redis 網路延遲卡住 Minecraft 主執行緒 (Server Thread)
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                Map<String, String> data = new HashMap<>();
                data.put("uuid", playerUuid.toString());
                data.put("name", playerName);
                data.put("message", message);

                String jsonMessage = GSON.toJson(data);
                jedis.publish(CHANNEL, jsonMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * 異步訂閱 Redis 頻道，收到訊息後顯示在伺服器上
     */
    public static void startListening(MinecraftServer server) {
        if (pool == null) return;

        subscribeThread = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                // 建立 Jedis 訂閱監聽器
                JedisPubSub pubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        try {
                            // 解析收到的 JSON
                            JsonObject json = GSON.fromJson(message, JsonObject.class);
                            String name = json.get("name").getAsString();
                            String msg = json.get("message").getAsString();

                            // 格式化訊息：[參賽者] playername: xxxxx
                            // §c 是紅色，§f 是白色，你可以自由更換顏色代碼
                            String formattedStr = "§c[參賽者] §f" + name + ": " + msg;
                            Text textComponent = Text.literal(formattedStr);

                            // 【核心】切換回 Minecraft 主執行緒廣播訊息給所有玩家
                            server.execute(() -> {
                                server.getPlayerManager().broadcast(textComponent, false);
                            });

                        } catch (Exception e) {
                            System.err.println("[Redis] 解析訊息失敗: " + e.getMessage());
                        }
                    }
                };

                System.out.println("[Redis] 開始監聽頻道: " + CHANNEL);
                jedis.subscribe(pubSub, CHANNEL);

            } catch (Exception e) {
                System.err.println("[Redis] 訂閱連線中斷，5秒後嘗試重連...");
                e.printStackTrace();
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                startListening(server); // 斷線重連機制
            }
        }, "Redis-Subscribe-Thread");

        subscribeThread.setDaemon(true); // 設定為守護執行緒，隨伺服器關閉而關閉
        subscribeThread.start();
    }

    // 伺服器關閉時呼叫
    public static void close() {
        if (pool != null) {
            pool.close();
        }
    }
}
