package org.auto.lucky_block_server_mod.command;

import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import org.auto.lucky_block_server_mod.clone_player_entity.ClonePlayerEntity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class cinematic_manager {
        // 儲存玩家 UUID 與 (結束 Tick, 對應克隆體) 的映射
    private static final Map<UUID, Long> endTickMap = new HashMap<>();
    private static final Map<UUID, ClonePlayerEntity> cloneMap = new HashMap<>();

    public static void startCinematicSequence(ServerPlayerEntity player, ClonePlayerEntity clone) {
        long endTick = player.getServer().getTicks() + 80; // 2秒後結束
        UUID uuid = player.getUuid();

        // 1. 初始設定
        player.changeGameMode(GameMode.SPECTATOR);

        // 偏移座標 (NPC 上方 70, 後方 30)
        double startX = clone.getX();
        double startY = clone.getY() + 50;
        double startZ = clone.getZ() - 80;

        player.teleport((net.minecraft.server.world.ServerWorld) clone.getWorld(),
                startX, startY, startZ, EnumSet.noneOf(PositionFlag.class), 0, 90, true);

        // 2. 設定運鏡鏡頭
        player.setCameraEntity(clone);

        // 3. 紀錄任務
        endTickMap.put(uuid, endTick);
        cloneMap.put(uuid, clone);
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        long currentTick = server.getTicks();

        // 檢查哪些玩家時間到了
        endTickMap.entrySet().removeIf(entry -> {
            if (currentTick >= entry.getValue()) {
                UUID uuid = entry.getKey();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                ClonePlayerEntity clone = cloneMap.remove(uuid);

                if (player != null && clone != null && !player.isDisconnected()) {
                    finishSequence(player, clone);
                }
                return true;
            }
            return false;
        });
    }

    private static void finishSequence(ServerPlayerEntity player, ClonePlayerEntity clone) {
        // 1. 先歸還鏡頭
        player.setCameraEntity(player);

        // 2. 傳送到 NPC 位置
        player.teleport(
                (net.minecraft.server.world.ServerWorld) clone.getWorld(),
                clone.getX(), clone.getY(), clone.getZ(),
                EnumSet.noneOf(PositionFlag.class),
                clone.getYaw(),
                clone.getPitch(),
                true
        );

        // 3. 切換模式
        player.changeGameMode(GameMode.SURVIVAL);

        // 4. --- 徹底清理 NPC ---
        // 移除區塊強載票券
        ChunkPos cp = new ChunkPos(clone.getBlockPos());
        ((ServerWorld)clone.getWorld()).getChunkManager().removeTicket(ChunkTicketType.FORCED, cp, 2, cp);

        // 移除實體與清單
        clone.discard();
        player.getServer().getPlayerManager().getPlayerList().remove(clone);
        // 斷開虛假連接，防止記憶體洩漏
        if (clone.networkHandler != null) {
            // 傳入一個包含原因的 DisconnectionInfo 對象
            clone.networkHandler.onDisconnected(new DisconnectionInfo(Text.literal("Sequence Finished")));
        }

        player.sendMessage(Text.literal("§6§lSYSTEM §f| §aConnection established. Sync complete."), true);
    }
}