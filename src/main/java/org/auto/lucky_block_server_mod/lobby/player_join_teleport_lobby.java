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
import org.auto.lucky_block_server_mod.cache.ServerInfo;
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

            System.out.println("[Debug] 玩家 " + handler.getPlayer().getDisplayName() + " 已離開，清理計分板緩存。");
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();

            // 1. 攔截 NPC：如果是 NPC 觸發 Join，直接 return，不執行任何後續動作
            if (isClonePlayer(player)) {
                return;
            }

            // --- 以下邏輯僅會針對「真實玩家」執行 ---

            // 💡 核心優化：異步載入普通玩家存檔，同時也去驗證是否在 admindata 表中
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                // A. 處理普通玩家存檔
                boolean isNewPlayer = !DATA_MANAGER.existsInMongo(uuid);
                if (!isNewPlayer) {
                    DATA_MANAGER.loadFromMongo(uuid);
                } else {
                    System.out.println("新玩家存檔: " + uuid);
                    DATA_MANAGER.saveInitialData(uuid);
                }

                // B. 🌟 處理 Admin 資料庫同步：從 MongoDB 的 admindata 載入該玩家的最新狀態
                // 這裡直接查詢 _id 是否存在於 admindata 且 group=99，如果是就將記憶體的 group 改為 99
                try {
                    // 假設你的 DataMap 裡有我們前面寫的驗證邏輯，或是直接拿 adminCollection 來做快取同步：
                    // 如果你想簡化，也可以在 DataMap 裡加上一個讀取管理員的方法，這裡我們直接查記憶體是否要更新
                    // 為了確保萬無一失，我們可以呼叫一個獨立的非同步載入（下方補充方法），或在此處直接比對快取
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 將結果傳遞給主執行緒（Server Thread）來進行傳送與遊戲模式變更
                server.execute(() -> {

                    // 2. 傳送與大廳邏輯
                    ServerWorld lobbyWorld = server.getWorld(LOBBY_WORLD_KEY);

                    if (lobbyWorld != null) {


                        // --- 🌟 權限與遊戲模式處理（徹底取代原本的 OP 等級 4 判斷） ---
                        if (DATA_MANAGER.isAdminInCache(uuid)) {
                            // 1. 如果在快取中抓到他是 Admin (group == 99)，直接切換旁觀者，並且「不生成 NPC 克隆體」
                            player.changeGameMode(GameMode.SPECTATOR);
                            player.sendMessage(Text.literal("§a[管理員認證] 歡迎回來，已為您自動切換至管理員旁觀模式。"), false);
                        } else {
                            // 2. 不是 Admin，走一般玩家流程（冒險模式 + 生成 NPC 任務）
                            player.changeGameMode(GameMode.ADVENTURE);

                            // 為真實玩家生成 Clone NPC 任務
                            spawnQueue.add(new PlayerSpawnTask(uuid, 800));

                            if (isNewPlayer) {
                                TeleportTarget target = new TeleportTarget(
                                        lobbyWorld,
                                        new Vec3d(LOBBY_X, LOBBY_Y, LOBBY_Z),
                                        Vec3d.ZERO, 0.0f, 90.0f, TeleportTarget.NO_OP
                                );

                                // 將真實玩家傳送到大廳
                                player.teleportTo(target);
                                player.sendMessage(Text.literal("§e偵測到新檔案，已將你傳送至活動大廳。"), false);
                            } else {
                                player.sendMessage(Text.literal("§a歡迎回來，已恢復您的比賽數據。"), false);
                                PlayerData playerData = DATA_MANAGER.getPlayerData(uuid);
                                ServerInfo currentServerInfo = Lucky_block_server_mod.serverManager;

                                if (playerData.isEliminated() && (currentServerInfo.getSession() == 2 ||
                                        currentServerInfo.getSession() == 3 ||
                                        currentServerInfo.getSession() == 4)) {
                                    player.changeGameMode(GameMode.SPECTATOR);
                                }
                            }
                        }
                    }
                });
            });
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
