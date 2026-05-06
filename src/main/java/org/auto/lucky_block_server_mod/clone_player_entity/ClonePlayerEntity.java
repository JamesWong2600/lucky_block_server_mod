package org.auto.lucky_block_server_mod.clone_player_entity;


import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class ClonePlayerEntity extends ServerPlayerEntity {


    // 儲存對應玩家的 UUID
    private final UUID ownerUuid;

    public ClonePlayerEntity(MinecraftServer server, ServerWorld world, GameProfile profile, UUID ownerUuid) {
        super(server, world, profile, SyncedClientOptions.createDefault());
        this.ownerUuid = ownerUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    /**
     * 修改後的隨機生成方法：傳入玩家對象進行綁定
     */
    public static ClonePlayerEntity spawnAtRandomTopPos(ServerPlayerEntity player, int radius) {
        MinecraftServer server = player.getServer();
        // 建議使用 player 所在的維度，而非鎖死 Overworld
        ServerWorld world = player.getServer().getOverworld();
        BlockPos center = player.getBlockPos();

        Random random = new Random();
        int randomX = center.getX() + (random.nextInt(radius * 2 + 1) - radius);
        int randomZ = center.getZ() + (random.nextInt(radius * 2 + 1) - radius);

        // 1. 強制載入區塊到 FULL 狀態 (這會確保 Heightmap 已建立)
        // getChunk 會阻塞直到區塊載入完成
        world.getChunk(randomX >> 4, randomZ >> 4, ChunkStatus.FULL, true);

        // 2. 使用 MOTION_BLOCKING 獲取最高點
        // 此時區塊已在記憶體中，getTopY 將回傳正確數值
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, randomX, randomZ);

        // 3. 安全邊界檢查
        if (topY < world.getBottomY()) {
            topY = world.getSeaLevel(); // 如果還是失敗，至少放在海平面
        }

        BlockPos targetPos = new BlockPos(randomX, topY, randomZ);

        return spawnClone(server, world, player.getName().getString(), player.getUuid(), targetPos);
    }

//    public static ClonePlayerEntity spawnClone(MinecraftServer server, ServerWorld world, String name, UUID ownerUuid, BlockPos pos) {
//        UUID cloneUuid = UUID.nameUUIDFromBytes(("CLONE_ENTITY:" + ownerUuid.toString()).getBytes());
//        GameProfile profile = new GameProfile(cloneUuid, name);
//
//        ClonePlayerEntity clone = new ClonePlayerEntity(server, world, profile, ownerUuid);
//        clone.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
//
//        ClientConnection fakeConnection = new ClientConnection(NetworkSide.SERVERBOUND);
//        clone.networkHandler = new ServerPlayNetworkHandler(server, fakeConnection, clone,
//                new ConnectedClientData(profile, 0, clone.getClientOptions(), false));
//
//        // 保持區塊載入
//        ChunkPos chunkPos = new ChunkPos(pos);
//        world.getChunkManager().addTicket(ChunkTicketType.FORCED, chunkPos, 2, chunkPos);
//
//        // --- 關鍵修正：必須加入 PlayerList 才能讓運鏡與封包正常運作 ---
//        server.getPlayerManager().getPlayerList().add(clone);
//        world.spawnEntity(clone);
//
//        return clone;
//    }
    public static ClonePlayerEntity spawnClone(MinecraftServer server, ServerWorld world, String name, UUID ownerUuid, BlockPos pos) {
        // 1. 嘗試從伺服器快取獲取完整的 GameProfile (包含皮膚數據)
        // 如果玩家在線上，這通常能直接拿到包含 Skin 屬性的 Profile
        Optional<GameProfile> existingProfile = server.getUserCache().getByUuid(ownerUuid);

        UUID cloneUuid = UUID.nameUUIDFromBytes(("CLONE_ENTITY:" + ownerUuid.toString()).getBytes());
        GameProfile profile = new GameProfile(cloneUuid, name);

        // 2. 複製皮膚屬性 (Properties)
        existingProfile.ifPresent(ownerProfile -> {
            profile.getProperties().putAll(ownerProfile.getProperties());
        });

        ClonePlayerEntity clone = new ClonePlayerEntity(server, world, profile, ownerUuid);
        clone.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);

        // --- 網路處理部分 ---
        ClientConnection fakeConnection = new ClientConnection(NetworkSide.SERVERBOUND);
        clone.networkHandler = new ServerPlayNetworkHandler(server, fakeConnection, clone,
                new ConnectedClientData(profile, 0, clone.getClientOptions(), false));

        // 保持區塊載入
        ChunkPos chunkPos = new ChunkPos(pos);
        world.getChunkManager().addTicket(ChunkTicketType.FORCED, chunkPos, 2, chunkPos);

        // 加入 PlayerList 並生成
        server.getPlayerManager().getPlayerList().add(clone);
        world.spawnEntity(clone);

        return clone;
    }
}