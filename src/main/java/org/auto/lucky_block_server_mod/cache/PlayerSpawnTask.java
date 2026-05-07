package org.auto.lucky_block_server_mod.cache;

import java.util.UUID;

public class PlayerSpawnTask {
    private final UUID playerUuid;
    private final int radius;

    /**
     * 建構子：創建一個新的任務包
     */
    public PlayerSpawnTask(UUID playerUuid, int radius) {
        this.playerUuid = playerUuid;
        this.radius = radius;
    }

    // Getter 方法
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getRadius() {
        return radius;
    }
}