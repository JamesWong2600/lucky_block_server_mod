package org.auto.lucky_block_server_mod.scoreboard;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.auto.lucky_block_server_mod.Lucky_block_server_mod;
import org.auto.lucky_block_server_mod.cache.PlayerData;
import org.auto.lucky_block_server_mod.cache.ServerInfo;
import org.auto.lucky_block_server_mod.data.player_amount;

import static org.auto.lucky_block_server_mod.data.player_amount.*;
import static org.auto.lucky_block_server_mod.flow.flow_controller.GetGameFlow;
import static org.auto.lucky_block_server_mod.flow.game_timer.getTotalSeconds;
import static org.auto.lucky_block_server_mod.lucky_block_data.LuckBlockData.getBrokenCount;

public class tick_scoreboard_handler {
    private static int tickCounter = 0;

    public static void timer_register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            tickCounter++;

            // 每 20 tick (1秒) 執行一次
            if (tickCounter >= 20) {
                tickCounter = 0;

                // 1. 更新全域人數到 Redis
                //player_amount.updateRedisCount(server);

                // 2. 獲取數據
                int local = getLocalPlayerAmount(server);
                int global = getGlobalPlayerAmount();

                ServerInfo currentServerInfo = Lucky_block_server_mod.serverManager;
                // 3. 更新所有玩家的計分板

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    PlayerData playerData = Lucky_block_server_mod.DATA_MANAGER.getPlayerData(player.getUuid());
                    int block_count = playerData.getBlockBreak();
                    int killCount = playerData.getKillCount();

                    if (currentServerInfo.getSession() == 1) {
                        EventScoreboard.updateScoreboard(
                                player,
                                "集合階段",
                                null,
                                null, // 假設不顯示破壞數
                                null,
                                (int) server.getOverworld().getWorldBorder().getMaxRadius(),
                                getGlobalPlayerAmount(),
                                getLocalPlayerAmount(server),
                                getConfig().server.id
                        );
                    }
                    if (currentServerInfo.getSession() == 2) {
                        EventScoreboard.updateScoreboard(
                                player,
                                "和平時期",
                                getTotalSeconds(),
                                block_count, // 假設不顯示破壞數
                                null,
                                (int) server.getOverworld().getWorldBorder().getMaxRadius(),
                                getGlobalPlayerAmount(),
                                getLocalPlayerAmount(server),
                                getConfig().server.id
                        );
                    }
                    if (currentServerInfo.getSession() == 3) {
                        EventScoreboard.updateScoreboard(
                                player,
                                "戰鬥時期",
                                getTotalSeconds(),
                                block_count, // 假設不顯示破壞數
                                killCount,
                                (int) server.getOverworld().getWorldBorder().getMaxRadius(),
                                getGlobalPlayerAmount(),
                                getLocalPlayerAmount(server),
                                getConfig().server.id
                        );
                    }
                    if (currentServerInfo.getSession() == 4) {
                        EventScoreboard.updateScoreboard(
                                player,
                                "游戲結束",
                                null,
                                block_count, // 假設不顯示破壞數
                                killCount,
                                (int) server.getOverworld().getWorldBorder().getMaxRadius(),
                                getGlobalPlayerAmount(),
                                getLocalPlayerAmount(server),
                                getConfig().server.id
                        );
                    }
                }
            }
        });
    }
}
