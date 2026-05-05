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
        ServerWorld world = player.getServerWorld();
        BlockPos center = player.getBlockPos();

        Random random = new Random();
        int randomX = center.getX() + (random.nextInt(radius * 2 + 1) - radius);
        int randomZ = center.getZ() + (random.nextInt(radius * 2 + 1) - radius);

        int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, randomX, randomZ);
        BlockPos targetPos = new BlockPos(randomX, topY, randomZ);

        // 使用玩家的名字和 UUID 來生成克隆體
        return spawnClone(server, world, player.getName().getString(), player.getUuid(), targetPos);
    }

    public static ClonePlayerEntity spawnClone(MinecraftServer server, ServerWorld world, String name, UUID ownerUuid, BlockPos pos) {
        UUID cloneUuid = UUID.nameUUIDFromBytes(("CLONE_ENTITY:" + ownerUuid.toString()).getBytes());
        GameProfile profile = new GameProfile(cloneUuid, name);

        ClonePlayerEntity clone = new ClonePlayerEntity(server, world, profile, ownerUuid);
        clone.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);

        ClientConnection fakeConnection = new ClientConnection(NetworkSide.SERVERBOUND);
        clone.networkHandler = new ServerPlayNetworkHandler(server, fakeConnection, clone,
                new ConnectedClientData(profile, 0, clone.getClientOptions(), false));

        // 保持區塊載入
        ChunkPos chunkPos = new ChunkPos(pos);
        world.getChunkManager().addTicket(ChunkTicketType.FORCED, chunkPos, 2, chunkPos);

        // --- 關鍵修正：必須加入 PlayerList 才能讓運鏡與封包正常運作 ---
        server.getPlayerManager().getPlayerList().add(clone);
        world.spawnEntity(clone);

        return clone;
    }
}