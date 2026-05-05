package org.auto.lucky_block_server_mod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.auto.lucky_block_server_mod.clone_player_entity.ClonePlayerEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.auto.lucky_block_server_mod.Lucky_block_server_mod.LOBBY_WORLD_KEY;
import static org.auto.lucky_block_server_mod.command.cinematic_manager.startCinematicSequence;
import static org.auto.lucky_block_server_mod.flow.flow_controller.StartGameFlow;


public class startcommand {
    public static void command_register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("start")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("seconds", IntegerArgumentType.integer(10, 3600))
                            .executes(context -> {
                                int seconds = IntegerArgumentType.getInteger(context, "seconds");
                                return startEvent(context.getSource(), seconds);
                            })
                    )
            );
        });
    }

    private static int startEvent(ServerCommandSource source, int seconds) {
        MinecraftServer server = source.getServer();
        ServerWorld world = server.getOverworld();

        System.out.println("[DEBUG] --- START EVENT SEQUENCE ---");

        try {
            // 1. 更新活動流程狀態 (假設這是在別處定義的方法)
            // StartGameFlow();

            // 2. 全服廣播
            server.getPlayerManager().broadcast(
                    Text.literal("§6§l[EVENT] §aMatch Started! §eTime Limit: " + seconds + "s"),
                    false
            );

            int successCount = 0;
            int fallbackCount = 0;

            // --- 關鍵修正：複製一份清單，避免 ConcurrentModificationException ---
            List<ServerPlayerEntity> participants = new ArrayList<>(server.getPlayerManager().getPlayerList());

            for (ServerPlayerEntity player : participants) {
                // 過濾掉已經是 Clone 的實體 (如果有的話)
                if (player instanceof ClonePlayerEntity) continue;

                String playerName = player.getName().getString();
                ClonePlayerEntity existingClone = null;

                // 搜尋屬於該玩家的克隆體
                for (var entity : world.iterateEntities()) {
                    if (entity instanceof ClonePlayerEntity c) {
                        if (c.getOwnerUuid() != null && c.getOwnerUuid().equals(player.getUuid())) {
                            existingClone = c;
                            break;
                        }
                    }
                }

                if (existingClone != null) {
                    System.out.println("[DEBUG] Found clone for " + playerName);
                    startCinematicSequence(player, existingClone);
                    successCount++;
                } else {
                    System.err.println("[DEBUG] No clone for " + playerName + ". Attempting spawn...");

                    if (world != null && player != null) {
                        // 這裡會觸發 spawnClone 並修改原始 PlayerList，但因為我們遍歷的是副本，所以安全
                        ClonePlayerEntity newClone = ClonePlayerEntity.spawnAtRandomTopPos(player, 800);
                        if (newClone != null) {
                            System.out.println("[DEBUG] Fallback spawn success for " + playerName);
                            startCinematicSequence(player, newClone);
                            fallbackCount++;
                        }
                    }
                }
            }

            // 3. 播放音效 (1.21.4 需要 .value())
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
            }

            // 4. 回饋指令執行者
            final int fSuccess = successCount;
            final int fFallback = fallbackCount;
            System.out.println("[DEBUG] --- Finished: Success=" + fSuccess + " Fallback=" + fFallback + " ---");
            source.sendFeedback(() -> Text.literal("§aEvent Started! Success: " + fSuccess + " Fallback: " + fFallback), true);

        } catch (Exception e) {
            System.err.println("[DEBUG] CRITICAL ERROR during startEvent:");
            e.printStackTrace();
            return 0;
        }
        return 1;
    }

    public static void startCinematicSequence(ServerPlayerEntity player, ClonePlayerEntity clone) {
        // 確保玩家 (攝影機) 變為旁觀者，克隆體 (被攝物) 變為生存模式
        player.changeGameMode(GameMode.SPECTATOR);
        clone.changeGameMode(GameMode.SURVIVAL);

        // 計算高空攝影機位置
        double startX = clone.getX();
        double startY = clone.getY() + 50;
        double startZ = clone.getZ() - 30;

        // 傳送「玩家本尊」到天上
        player.teleport((ServerWorld) clone.getWorld(),
                startX, startY, startZ, EnumSet.noneOf(PositionFlag.class), 0, 90, true);

        // 核心：玩家看著克隆體
        player.setCameraEntity(clone);

        // 註冊到 Tick 任務中 (假設你有一個 CinematicManager 處理這個)
        // CinematicManager.register(player, clone);
    }
}
