package org.auto.lucky_block_server_mod.scoreboard;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.auto.lucky_block_server_mod.data.player_amount;

import static org.auto.lucky_block_server_mod.flow.flow_controller.GetGameFlow;

public class tick_scoreboard_handler {
    private static int tickCounter = 0;

    public static void timer_register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            tickCounter++;

            // 每 20 tick (1秒) 執行一次
            if (tickCounter >= 20) {
                tickCounter = 0;

                // 1. 更新全域人數到 Redis
                player_amount.updateRedisCount(server);

                // 2. 獲取數據
                int local = player_amount.getLocalPlayerAmount(server);
                int global = player_amount.getGlobalPlayerAmount();

                // 3. 更新所有玩家的計分板
                if(GetGameFlow()==1){
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    EventScoreboard.updateScoreboard(
                            player,
                            null,
                            null,
                            null, // 假設不顯示破壞數
                            (int) server.getOverworld().getWorldBorder().getSize(),
                            "全域人數: " + global // 將全域人數顯示在分組欄位
                    );
                }
            }
            }
        });
    }
}
