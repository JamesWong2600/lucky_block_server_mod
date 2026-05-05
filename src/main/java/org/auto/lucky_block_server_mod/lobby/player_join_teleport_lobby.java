package org.auto.lucky_block_server_mod.lobby;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.auto.lucky_block_server_mod.scoreboard.EventScoreboard;

import java.util.EnumSet;

public class player_join_teleport_lobby {


        // 設定大廳座標
        private static final double LOBBY_X = 0.5;
        private static final double LOBBY_Y = 100.0;
        private static final double LOBBY_Z = 0.5;
        private static final float LOBBY_YAW = 0.0f;
        private static final float LOBBY_PITCH = 0.0f;

        public static void lobby_teleport_register() {
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                ServerPlayerEntity player = handler.player;
                ServerWorld world = server.getOverworld(); // 假設大廳在主世界

                // 1. 執行傳送
                // 使用 teleport 方法可以確保跨維度也能正確處理
                player.teleport(
                        world,
                        LOBBY_X, LOBBY_Y, LOBBY_Z,
                        EnumSet.noneOf(PositionFlag.class), // 標記：不使用相對座標
                        LOBBY_YAW,
                        LOBBY_PITCH,
                        true // resetCamera: 是否重置視角
                );

                // 2. 發送歡迎訊息
                player.sendMessage(Text.literal("§e歡迎參加幸運方塊活動！已將你傳送至大廳。"), false);

                // 3. 初始化計分板 (避免玩家進來看到空的側邊欄)
                // 這裡傳入初始數值
                EventScoreboard.updateScoreboard(
                        player,
                        "準備中",
                        null,
                        null,
                        null,

                        "未分組"
                );
            });
        }
}
