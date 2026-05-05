package org.auto.lucky_block_server_mod.command;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import org.auto.lucky_block_server_mod.clone_player_entity.ClonePlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class cinematic_manager {
        // 儲存玩家 UUID 與 (結束 Tick, 對應克隆體) 的映射
        // 使用 ConcurrentHashMap 確保 Thread-safe
    private static final Map<UUID, Long> endTickMap = new ConcurrentHashMap<>();
    private static final Map<UUID, ClonePlayerEntity> cloneMap = new ConcurrentHashMap<>();
    private static final Set<UUID> transitioningPlayers = ConcurrentHashMap.newKeySet();
    /**
     * 開始電影序幕
     * @param durationTicks 持續時間 (20 ticks = 1秒)
     */


    public static void startCinematicSequence(ServerPlayerEntity player, ClonePlayerEntity clone, int durationTicks) {
        if (player.isRemoved() || !clone.isAlive()) return;
        // --- 1. 背包處理：先清空，再給鑽石鎬 ---
        player.getInventory().clear(); // 清空玩家所有物品、裝備與副手

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        player.getInventory().insertStack(pickaxe); // 放入鑽石鎬

        player.sendMessage(Text.literal("§c§l[!] §fInventory cleared! Prepared for mining."), false);

        UUID uuid = player.getUuid();
        endTickMap.put(uuid, player.getServer().getTicks() + (long) durationTicks);
        cloneMap.put(uuid, clone);
        transitioningPlayers.add(uuid); // 開啟運鏡動畫

        // 1.21.4 安全傳送流程 (保持不變)
        double startX = clone.getX();
        double startY = clone.getY() + 40; // 從上方 40 格開始俯衝
        double startZ = clone.getZ() - 60;

        TeleportTarget target = new TeleportTarget(
                (ServerWorld) clone.getWorld(),
                new Vec3d(startX, startY, startZ),
                Vec3d.ZERO, 0.0f, 90.0f, TeleportTarget.NO_OP
        );

        player.getServer().execute(() -> {
            player.teleportTo(target);
            player.getServer().execute(() -> {
                player.changeGameMode(GameMode.SPECTATOR);
                // 這裡暫時不 setCameraEntity，改由 tick 處理平滑移動
            });
        });
    }

    public static void tick(MinecraftServer server) {
        long currentTick = server.getTicks();

        // 處理運鏡縮進效果
        for (UUID uuid : transitioningPlayers) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            ClonePlayerEntity clone = cloneMap.get(uuid);

            if (player != null && clone != null) {
                moveCameraTowardsClone(player, clone);
            }
        }

        // 處理結束邏輯 (保持不變)
        Iterator<Map.Entry<UUID, Long>> iterator = endTickMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (currentTick >= entry.getValue()) {
                UUID uuid = entry.getKey();
                finishSequence(server.getPlayerManager().getPlayer(uuid), cloneMap.remove(uuid));
                transitioningPlayers.remove(uuid);
                iterator.remove();
            }
        }
    }

    private static void moveCameraTowardsClone(ServerPlayerEntity player, ClonePlayerEntity clone) {
        Vec3d playerPos = player.getPos();
        Vec3d targetPos = clone.getEyePos(); // 目標是克隆體的眼睛

        double distance = playerPos.distanceTo(targetPos);

        if (distance < 0.5) {
            // 距離足夠近了，正式「進入身體」鎖定視角
            player.setCameraEntity(clone);
            transitioningPlayers.remove(player.getUuid());
            return;
        }

        // 計算下一幀的位置 (線性內插 Lerp)
        // 數值越小運鏡越慢，建議 0.1 ~ 0.2
        double lerpFactor = 0.1;
        double nextX = MathHelper.lerp(lerpFactor, playerPos.x, targetPos.x);
        double nextY = MathHelper.lerp(lerpFactor, playerPos.y, targetPos.y);
        double nextZ = MathHelper.lerp(lerpFactor, playerPos.z, targetPos.z);

        // 計算朝向，讓鏡頭始終盯著克隆體
        Vec3d diff = targetPos.subtract(playerPos);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;
        float pitch = (float) Math.toDegrees(-Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));

        // 使用 networkHandler 進行平滑移動，不使用 teleport 以免觸發加載開銷
        player.networkHandler.requestTeleport(nextX, nextY, nextZ, yaw, pitch);
    }

    /**
     * 結束序幕並清理實體
     */
    private static void finishSequence(ServerPlayerEntity player, ClonePlayerEntity clone) {
        if (player == null || clone == null) return;

        // 1. 先歸還鏡頭控制權
        player.setCameraEntity(player);

        // 2. 構建 TeleportTarget 以取代舊有的 teleport 方法
        // 目標是克隆體當前的位置與旋轉角度
        TeleportTarget finishTarget = new TeleportTarget(
                (ServerWorld) clone.getWorld(),
                clone.getPos(),
                player.getVelocity(), // 保持玩家當前的動量
                clone.getYaw(),
                clone.getPitch(),
                TeleportTarget.NO_OP  // 傳送後不執行額外動作
        );

        // 執行 1.21.4 推薦的傳送方式
        player.teleportTo(finishTarget);

        // 3. 切換回生存模式
        player.changeGameMode(GameMode.SURVIVAL);

        // 4. --- 徹底清理 NPC 與 內存 ---
        ServerWorld world = (ServerWorld) clone.getWorld();
        ChunkPos cp = new ChunkPos(clone.getBlockPos());

        // 移除區塊票券
        world.getChunkManager().removeTicket(ChunkTicketType.FORCED, cp, 2, cp);

        // 徹底移除克隆體
        clone.discard();

        // 清理虛假玩家列表 (NPC 專用)
        player.getServer().getPlayerManager().getPlayerList().remove(clone);

        if (clone.networkHandler != null) {
            // 1.21.4 的斷開連接處理
            clone.networkHandler.onDisconnected(new DisconnectionInfo(Text.literal("Sequence Finished")));
        }

        player.sendMessage(Text.literal("§6§lSYSTEM §f| §aConnection established. Sync complete."), true);
    }
}