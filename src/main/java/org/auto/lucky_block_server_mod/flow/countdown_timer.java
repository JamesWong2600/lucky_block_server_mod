package org.auto.lucky_block_server_mod.flow;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.auto.lucky_block_server_mod.command.startcommand;

import static org.auto.lucky_block_server_mod.flow.flow_controller.StartGameFlow;
import static org.auto.lucky_block_server_mod.flow.game_timer.startTimer;

public class countdown_timer {
    private static int timeLeft = -1;
    private static int tickCounter = 0;
    // 儲存啟動指令的來源，以便後續調用
    private static ServerCommandSource savedSource;

    public static void startCountdown(ServerCommandSource source) {
        timeLeft = 10;
        tickCounter = 0;
        savedSource = source; // 儲存 source
    }

    public static void tick(MinecraftServer server) {
        if (timeLeft < 0) return;

        tickCounter++;

        if (tickCounter >= 20) {
            tickCounter = 0;

            if (timeLeft > 0) {
                broadcastCountdown(server, timeLeft);
                timeLeft--;
            } else {
                // 時間到，將儲存的 source 傳入
                onTimerEnd(server, savedSource);
                timeLeft = -1;
                savedSource = null; // 清空引用防止記憶體洩漏
            }
        }
    }

    private static void broadcastCountdown(MinecraftServer server, int seconds) {
        String color = (seconds <= 3) ? "§c" : "§e";
        Text message = Text.literal("§6§l[活動] §f比賽倒數: " + color + seconds + " §f秒");

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(message, true);
            // 1.21.4 播放音效需使用 .value()
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.MASTER, 1.0f, 1.0f);
        }
    }

    private static void onTimerEnd(MinecraftServer server, ServerCommandSource source) {
        server.getPlayerManager().broadcast(Text.literal("§a§l比賽開始！ GO! GO! GO!"), false);

        // 如果 source 遺失（例如伺服器重啟後計時器仍在跑），則使用預設來源
        ServerCommandSource effectiveSource = (source != null) ? source : server.getCommandSource();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.MASTER, 1.0f, 1.0f);
        }
        StartGameFlow();
        startTimer();
        // 呼叫你的 startEvent 並傳入 300 秒（假設預設 5 分鐘）
        startcommand.startEvent(effectiveSource, 300);
    }

    public static int getTimeLeft() {
        return timeLeft;
    }
}