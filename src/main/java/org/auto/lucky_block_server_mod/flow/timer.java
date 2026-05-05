package org.auto.lucky_block_server_mod.flow;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class timer {
    private static int timeLeft = -1; // -1 代表計時器未啟動
    private static int tickCounter = 0;

    public static void startCountdown(int seconds) {
        timeLeft = seconds;
        tickCounter = 0;
    }

    public static void tick(MinecraftServer server) {
        if (timeLeft < 0) return;

        tickCounter++;

        // 每 20 tick (1秒) 執行一次
        if (tickCounter >= 20) {
            tickCounter = 0;

            if (timeLeft > 0) {
                // 廣播倒數訊息與音效
                broadcastCountdown(server, timeLeft);
                timeLeft--;
            } else {
                // 時間到，觸發開場邏輯
                onTimerEnd(server);
                timeLeft = -1;
            }
        }
    }

    private static void broadcastCountdown(MinecraftServer server, int seconds) {
        String color = (seconds <= 3) ? "§c" : "§e"; // 最後三秒變紅色
        Text message = Text.literal("§6§l[活動] §f比賽倒數: " + color + seconds + " §f秒");

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // 顯示在 ActionBar (物品欄上方) 比較不會擋住畫面
            player.sendMessage(message, true);

            // 播放滴答聲
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.MASTER, 1.0f, 1.0f);
        }
    }

    private static void onTimerEnd(MinecraftServer server) {
        server.getPlayerManager().broadcast(Text.literal("§a§l比賽開始！ GO! GO! GO!"), false);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.MASTER, 1.0f, 1.0f);

            // 這裡可以呼叫你之前的運鏡邏輯
            // CinematicManager.startCinematicSequence(player, ...);
        }
    }

    public static int getTimeLeft() {
        return timeLeft;
    }
}
