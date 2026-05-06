package org.auto.lucky_block_server_mod.cache;

import com.mongodb.client.model.ReplaceOptions;
import org.auto.lucky_block_server_mod.Lucky_block_server_mod;
import org.bson.Document;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.auto.lucky_block_server_mod.flow.flow_controller.GetGameFlow;

public class CooldownManager {
    // 改用 ConcurrentHashMap 確保線程安全
    private static final Map<UUID, Long> disconnectTimeCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> disconnectCountCache = new ConcurrentHashMap<>();

    public static void onPlayerDisconnect(UUID uuid) {
        if (GetGameFlow() == 2) {
            long now = System.currentTimeMillis();
            int counts = disconnectCountCache.getOrDefault(uuid, 0) + 1;

            disconnectTimeCache.put(uuid, now);
            disconnectCountCache.put(uuid, counts);

            // 統一呼叫 DATA_MANAGER 的方法，不要自己寫 flushToMongo
            Lucky_block_server_mod.DATA_MANAGER.saveCooldown(uuid, now, counts, isEliminated(uuid));
        }
    }

    public static boolean isEliminated(UUID uuid) {
        int counts = disconnectCountCache.getOrDefault(uuid, 0);
        if (counts > 3) return true;

        if (disconnectTimeCache.containsKey(uuid)) {
            long goneTime = (System.currentTimeMillis() - disconnectTimeCache.get(uuid)) / 1000;
            if (goneTime > 60) return true;
        }
        return false;
    }

    public static void clearDisconnectTime(UUID uuid) {
        disconnectTimeCache.remove(uuid);
        // 連回後更新資料庫，標記已在線上 (timestamp 設為 0)
        Lucky_block_server_mod.DATA_MANAGER.saveCooldown(uuid, 0, disconnectCountCache.getOrDefault(uuid, 0), false);
    }

    // 刪除了原本報錯的 flushToMongo 方法，改由 DataMap 負責

    public static void checkTimeouts() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = disconnectTimeCache.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID uuid = entry.getKey();
            long disconnectTime = entry.getValue();

            if ((now - disconnectTime) / 1000 > 60) {
                System.out.println("玩家 " + uuid + " 已斷線超過 60 秒，正式淘汰！");
                int counts = disconnectCountCache.getOrDefault(uuid, 0);

                // 呼叫主控數據類
                Lucky_block_server_mod.DATA_MANAGER.saveCooldown(uuid, disconnectTime, counts, true);
                iterator.remove();
            }
        }
    }
}