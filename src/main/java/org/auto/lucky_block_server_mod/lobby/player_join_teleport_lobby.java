package org.auto.lucky_block_server_mod.lobby;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import org.auto.lucky_block_server_mod.Lucky_block_server_mod;
import org.auto.lucky_block_server_mod.cache.DataMap;
import org.auto.lucky_block_server_mod.cache.PlayerData;
import org.auto.lucky_block_server_mod.cache.PlayerSpawnTask;
import org.auto.lucky_block_server_mod.clone_player_entity.ClonePlayerEntity;
import org.auto.lucky_block_server_mod.scoreboard.EventScoreboard;

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.ibm.icu.text.CurrencyMetaInfo.hasData;
import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.LOBBY_WORLD_KEY;
import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.spawnQueue;

public class player_join_teleport_lobby {


        // 設定大廳座標
        private static final double LOBBY_X = 0.5;
        private static final double LOBBY_Y = 5.0;
        private static final double LOBBY_Z = 0.5;
        private static final float LOBBY_YAW = 0.0f;
        private static final float LOBBY_PITCH = 0.0f;

        public static final DataMap DATA_MANAGER = new DataMap();

    private static boolean isClonePlayer(ServerPlayerEntity player) {
        // 直接檢查加入的玩家是否是 ClonePlayerEntity 類型
        return player instanceof ClonePlayerEntity;
    }

    public static void lobby_teleport_register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // 獲取斷開連線玩家的 UUID
            java.util.UUID playerUuid = handler.getPlayer().getUuid();

            // 1. 調用你 EventScoreboard 裡的重置方法
            EventScoreboard.resetPlayer(playerUuid);

            // 或是直接在事件中清理（如果你把變數設為 public）
            // EventScoreboard.initializedPlayers.remove(playerUuid);
            // EventScoreboard.playerLastLines.remove(playerUuid);

            System.out.println("[Debug] 玩家 " + handler.getPlayer().getDisplayName() + " 已離開，清理計分板緩存。");
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();

            // 1. 攔截 NPC：如果是 NPC 觸發 Join，直接 return，不執行任何後續動作
            // 這裡檢查主世界是否有該 NPC 實體
            if (isClonePlayer(player)) {
                return;
            }

            // --- 以下邏輯僅會針對「真實玩家」執行 ---

            final boolean[] isNewPlayerWrapper = {false};

            // 真實玩家資料庫處理
            isNewPlayerWrapper[0] = !DATA_MANAGER.existsInMongo(uuid);
            if (!isNewPlayerWrapper[0]) {
                DATA_MANAGER.loadFromMongo(uuid);
            } else {
                System.out.println("新玩家存檔: " + uuid);
                DATA_MANAGER.saveInitialData(uuid);
            }

            final boolean isNewPlayer = isNewPlayerWrapper[0];

            // 2. 傳送與大廳邏輯
            ServerWorld lobbyWorld = server.getWorld(LOBBY_WORLD_KEY);

            if (lobbyWorld != null) {
                TeleportTarget target = new TeleportTarget(
                        lobbyWorld,
                        new Vec3d(LOBBY_X, LOBBY_Y, LOBBY_Z),
                        Vec3d.ZERO, 0.0f, 90.0f, TeleportTarget.NO_OP
                );

                server.execute(() -> {
                    // 將真實玩家傳送到大廳
                    player.teleportTo(target);

                    // --- 權限與遊戲模式處理 ---
                    if (player.hasPermissionLevel(4)) {
                        player.changeGameMode(GameMode.SPECTATOR);
                    } else {
                        player.changeGameMode(GameMode.ADVENTURE);

                        // 為真實玩家生成 Clone NPC 任務
                        spawnQueue.add(new PlayerSpawnTask(uuid, 800));

                        if (isNewPlayer) {
                            player.sendMessage(Text.literal("§e偵測到新檔案，已將你傳送至活動大廳。"), false);
                        } else {
                            player.sendMessage(Text.literal("§a歡迎回來，已恢復您的比賽數據。"), false);
                        }
                    }
                });
            }
        });

//            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
//                UUID uuid = handler.player.getUuid();
//                ServerPlayerEntity player = handler.player;
//
//                // 1. 在主線程先確保記憶體對象存在，防止 Flush 跳過，但先不載入資料庫數據
//                DATA_MANAGER.getPlayerData(uuid);
//
//                //CompletableFuture.runAsync(() -> {
//                    // --- 判斷是否為新玩家 (以資料庫為準) ---
//                    boolean isNewPlayer = !DATA_MANAGER.existsInMongo(uuid);
//
//                    System.out.println("notexist");
//
//                    if (!isNewPlayer) {
//                        // 情況：老玩家，從資料庫同步最新數據到記憶體
//                        DATA_MANAGER.loadFromMongo(uuid);
//                    } else {
//                        // 情況：真正的新玩家，立即在資料庫建立初始存檔
//                        // 避免這 5 秒內伺服器崩潰導致名單遺失
//                        System.out.println("save");
//                        DATA_MANAGER.saveInitialData(uuid);
//                    }
//
//                    // 2. 根據判斷結果執行後續邏輯
//                    if (isNewPlayer) {
//                        ServerWorld world = server.getWorld(LOBBY_WORLD_KEY);
//                        if (world != null) {
//                            TeleportTarget target = new TeleportTarget(
//                                    world,
//                                    new Vec3d(LOBBY_X, LOBBY_Y, LOBBY_Z),
//                                    Vec3d.ZERO, 0.0f, 90.0f, TeleportTarget.NO_OP
//                            );
//
//                            server.execute(() -> {
//                                // OP 豁免
//                                if (player.hasPermissionLevel(4)){
//                                    player.teleportTo(target);
//                                    player.changeGameMode(GameMode.SPECTATOR);
//                                    return;
//                                }
//
//                                player.teleportTo(target);
//                                player.changeGameMode(GameMode.ADVENTURE);
//
//                                // 生成活動實體
//                                ClonePlayerEntity.spawnAtRandomTopPos(player, 800);
//                                player.sendMessage(Text.literal("§e偵測到新檔案，已將你傳送至活動大廳。"), false);
//                            });
//                        }
//                    } else {
//                        player.sendMessage(Text.literal("§a歡迎回來，已恢復您的比賽數據。"), false);
//                    }
//                });
//            //});
       }
}
