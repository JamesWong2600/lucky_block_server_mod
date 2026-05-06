package org.auto.lucky_block_server_mod.cache;

import java.util.UUID;



public class PlayerData {
    public final UUID uuid;
    public boolean eliminated = false;
    public int blockBreak = 0;
    public int killCount = 0;
    public int group = 0;
    public long firstJoinTime = 0;  // 首次加入時間
    public long lastUpdated = 0;     // 最後更新時間

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.firstJoinTime = System.currentTimeMillis();
        this.lastUpdated = this.firstJoinTime;
    }

    // Getter 和 Setter 方法（可選，但建議添加）
    public UUID getUuid() { return uuid; }
    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }

    public int getBlockBreak() { return blockBreak; }
    public void setBlockBreak(int blockBreak) { this.blockBreak = blockBreak; }
    public void incrementBlockBreak() { this.blockBreak++; }

    public int getKillCount() { return killCount; }
    public void setKillCount(int killCount) { this.killCount = killCount; }
    public void incrementKillCount() { this.killCount++; }

    public int getGroup() { return group; }
    public void setGroup(int group) { this.group = group; }

    public long getFirstJoinTime() { return firstJoinTime; }
    public long getLastUpdated() { return lastUpdated; }
    public void updateLastUpdated() { this.lastUpdated = System.currentTimeMillis(); }
}