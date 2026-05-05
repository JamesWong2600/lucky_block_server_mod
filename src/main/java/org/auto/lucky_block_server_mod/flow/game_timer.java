package org.auto.lucky_block_server_mod.flow;

import net.minecraft.server.MinecraftServer;

public class game_timer {
    private static int totalSeconds = 0;
    private static int tickCounter = 0;
    private static boolean isRunning = false;

    // 開始累加計時
    public static void startTimer() {
        totalSeconds = 0;
        tickCounter = 0;
        isRunning = true;
    }

    // 停止計時
    public static void stopTimer() {
        isRunning = false;
    }

    // 重置計時
    public static void resetTimer() {
        totalSeconds = 0;
        tickCounter = 0;
    }

    // 在 ServerTickEvents.END_SERVER_TICK 中呼叫
    public static void game_tick(MinecraftServer server) {
        if (!isRunning) return;

        tickCounter++;

        // 每 20 ticks 增加 1 秒
        if (tickCounter >= 20) {
            totalSeconds++;
            tickCounter = 0;
        }
    }

    // 獲取原始秒數 (用於邏輯判斷)
    public static int getTotalSeconds() {
        return totalSeconds;
    }

    // 獲取格式化後的時間字串 (例如 "05:23")，用於 Scoreboard
    public static String getFormattedTime() {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static boolean isRunning() {
        return isRunning;
    }
}