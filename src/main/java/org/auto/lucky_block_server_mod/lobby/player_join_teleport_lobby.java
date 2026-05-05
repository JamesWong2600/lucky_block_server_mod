package org.auto.lucky_block_server_mod.lobby;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.auto.lucky_block_server_mod.clone_player_entity.ClonePlayerEntity;
import org.auto.lucky_block_server_mod.scoreboard.EventScoreboard;

import java.util.EnumSet;

import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.LOBBY_WORLD_KEY;

public class player_join_teleport_lobby {


        // 設定大廳座標
        private static final double LOBBY_X = 0.5;
        private static final double LOBBY_Y = 10.0;
        private static final double LOBBY_Z = 0.5;
        private static final float LOBBY_YAW = 0.0f;
        private static final float LOBBY_PITCH = 0.0f;

        public static void lobby_teleport_register() {
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                ServerPlayerEntity player = handler.player;
                ServerWorld world = server.getWorld(LOBBY_WORLD_KEY); // 假設大廳在主世界

                player.changeGameMode(GameMode.ADVENTURE);
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
                ClonePlayerEntity.spawnAtRandomTopPos(player, 800);
                // 2. 發送歡迎訊息
                player.sendMessage(Text.literal("§e歡迎參加幸運方塊活動！已將你傳送至大廳。"), false);

                // 3. 初始化計分板 (避免玩家進來看到空的側邊欄)
                // 這裡傳入初始數值
//                EventScoreboard.updateScoreboard(
//                        player,
//                        "準備中",
//                        null,
//                        null,
//                        null,
//
//                        "未分組"
//                );
            });
        }
}
