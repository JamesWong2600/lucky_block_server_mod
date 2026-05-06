package org.auto.lucky_block_server_mod.performance_stat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class NetWorkStuff {
    public static double getCurrentMspt(MinecraftServer server) {
        // 獲取伺服器紀錄所有 Tick 耗時的陣列 (奈秒)
        long[] times = server.getTickTimes();

        // 取得陣列中最新的一筆數據
        // 注意：times 是一個循環陣列，通常最新的數據在最後一個索引
        if (times == null || times.length == 0) return 0.0;

        long latestNanos = times[times.length - 1];

        // 轉換為毫秒 (ms)
        return latestNanos / 1_000_000.0;
    }

    public static int getPlayerPing(ServerPlayerEntity player) {
        if (player == null || player.networkHandler == null) {
            return 0;
        }

        // 在 1.21.4 中，ping 值直接存放在 ServerPlayerEntity 中
        // 這是伺服器與客戶端之間心跳包的往返時間
        return player.networkHandler.getLatency();
    }
}
