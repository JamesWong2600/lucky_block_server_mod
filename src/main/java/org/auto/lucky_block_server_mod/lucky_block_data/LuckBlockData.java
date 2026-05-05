package org.auto.lucky_block_server_mod.lucky_block_data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LuckBlockData {
    // 將 Map 移到這裡
    private static final Map<UUID, Integer> brokenBlocksMap = new ConcurrentHashMap<>();

    public static void addCount(UUID uuid) {
        brokenBlocksMap.put(uuid, brokenBlocksMap.getOrDefault(uuid, 0) + 1);
    }

    public static int getBrokenCount(UUID uuid) {
        return brokenBlocksMap.getOrDefault(uuid, 0);
    }

    public static void reset() {
        brokenBlocksMap.clear();
    }
}