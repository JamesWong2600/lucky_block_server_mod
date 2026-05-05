package org.auto.lucky_block_server_mod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import org.auto.lucky_block_server_mod.clone_player_entity.ClonePlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.auto.lucky_block_server_mod.clone_player_entity.ClonePlayerEntity.spawnClone;
import static org.auto.lucky_block_server_mod.command.cinematic_manager.startCinematicSequence;
import static org.auto.lucky_block_server_mod.flow.flow_controller.StartGameFlow;
import static org.auto.lucky_block_server_mod.flow.countdown_timer.startCountdown;
import static org.auto.lucky_block_server_mod.flow.game_timer.startTimer;


public class startcommand {
    public static void command_register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("start")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("seconds", IntegerArgumentType.integer(10, 3600))
                            .executes(context -> {
                                startCountdown(context.getSource()); // 傳入當前的 source

                                return 1;
                            })
                    )
            );
        });
    }
    public static int startEvent(ServerCommandSource source, int seconds) {
        MinecraftServer server = source.getServer();
        ServerWorld world = server.getOverworld();

        try {

            server.getPlayerManager().broadcast(Text.literal("§6§l[EVENT] §aMatch Started!"), false);

            List<ServerPlayerEntity> participants = new ArrayList<>(server.getPlayerManager().getPlayerList());

            for (ServerPlayerEntity player : participants) {
                if (player instanceof ClonePlayerEntity || player.isRemoved()) continue;

                // 搜尋現有的克隆體
                ClonePlayerEntity existingClone = findExistingClone(world, player);

                if (existingClone != null) {
                    // 已有克隆體，直接進入鏡頭
                    startCinematicSequence(player, existingClone,120);
                } else {
                    // 沒有克隆體，啟動非同步生成流程
                    spawnAtRandomTopPosAsync(player, 800).thenAccept(newClone -> {
                        if (newClone != null) {
                            // 這裡已經回到伺服器主線程，可以安全操作
                            startCinematicSequence(player, newClone,120);
                        }
                    }).exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
                }
            }

            // 播放音效等後續邏輯...
            source.sendFeedback(() -> Text.literal("§aEvent Process Started!"), true);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        return 1;
    }

    // 輔助方法：尋找克隆體
    private static ClonePlayerEntity findExistingClone(ServerWorld world, ServerPlayerEntity player) {
        for (var entity : world.iterateEntities()) {
            if (entity instanceof ClonePlayerEntity c) {
                if (player.getUuid().equals(c.getOwnerUuid())) return c;
            }
        }
        return null;
    }
    public static CompletableFuture<ClonePlayerEntity> spawnAtRandomTopPosAsync(ServerPlayerEntity player, int radius) {
        MinecraftServer server = player.getServer();
        ServerWorld world = player.getServerWorld();
        BlockPos center = player.getBlockPos();

        Random random = new Random();
        int randomX = center.getX() + (random.nextInt(radius * 2 + 1) - radius);
        int randomZ = center.getZ() + (random.nextInt(radius * 2 + 1) - radius);
        ChunkPos chunkPos = new ChunkPos(randomX >> 4, randomZ >> 4);

        // 1. 向伺服器請求非同步載入區塊
        return world.getChunkManager()
                .getChunkFutureSyncOnMainThread(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true)
                .thenApplyAsync(either -> {
                    // 2. 這裡已經是在主線程執行，且區塊已載入完成
                    int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, randomX, randomZ);
                    if (topY < world.getBottomY()) topY = world.getSeaLevel();

                    BlockPos targetPos = new BlockPos(randomX, topY, randomZ);

                    // 3. 呼叫原本的生成方法
                    return spawnClone(server, world, player.getName().getString(), player.getUuid(), targetPos);
                }, server); // 確保在伺服器主線程回調
    }


//    public static void startCinematicSequence(ServerPlayerEntity player, ClonePlayerEntity clone) {
//        // 1. 立即更改模式與傳送
//        player.changeGameMode(GameMode.SPECTATOR);
//        clone.changeGameMode(GameMode.SURVIVAL);
//
//        double startX = clone.getX();
//        double startY = clone.getY() + 50;
//        double startZ = clone.getZ() - 30;
//
//        player.teleport((ServerWorld) clone.getWorld(),
//                startX, startY, startZ, EnumSet.noneOf(PositionFlag.class), 0, 90, true);
//
//        // 2. 關鍵修正：將「設定攝影機」延遲到下一 Tick 執行
//        // 這能確保 Teleport 和 Spawn 的實體已經在 SectionManager 中完全註冊
//        player.getServer().execute(() -> {
//            if (player.isAlive() && clone.isAlive()) {
//                player.setCameraEntity(clone);
//                System.out.println("[DEBUG] Camera linked for " + player.getName().getString());
//            }
//        });
//    }
}
