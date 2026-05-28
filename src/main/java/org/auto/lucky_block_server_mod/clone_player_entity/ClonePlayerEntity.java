package org.auto.lucky_block_server_mod.clone_player_entity;


import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import org.auto.lucky_block_server_mod.Lucky_block_server_mod;
import org.auto.lucky_block_server_mod.cache.PlayerData;

import java.util.*;

public class ClonePlayerEntity extends ServerPlayerEntity {


    // 儲存對應玩家的 UUID
    private final UUID ownerUuid;
    private static final int MAX_DISPLAY_LENGTH = 16;
    private static final String NPC_PREFIX = "§z_";

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

        System.out.println("server: "+server);
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


        System.out.println("sucess");

        return spawnClone(server, world, safeTruncate(player.getName().getString()), player.getUuid(), targetPos);
    }


    public static String safeTruncate(String originalString) {
        if (originalString == null || originalString.isEmpty()) {
            return "";
        }

        // 1. 先加上獨特的「非玩家」標記
        // 這樣就算玩家叫 "Player123"，NPC 就會叫 "§z_Player123"
        String markedString = NPC_PREFIX + originalString;

        // 2. 如果加上標記後超過長度限制，進行裁切
        // 注意：顏色代碼在 Minecraft 內部也佔字元數
        if (markedString.length() > MAX_DISPLAY_LENGTH) {
            return markedString.substring(0, MAX_DISPLAY_LENGTH);
        } else {
            return markedString;
        }
    }

//    public static ClonePlayerEntity spawnClone(MinecraftServer server, ServerWorld world, String name, UUID ownerUuid, BlockPos pos) {
//        // 1. Profile 獲取 (這個部分保持不變，因為我們需要皮膚數據)
//        Optional<GameProfile> existingProfile = server.getUserCache().getByUuid(ownerUuid);
//        UUID cloneUuid = UUID.nameUUIDFromBytes(("STATIC_ENTITY:" + ownerUuid.toString()).getBytes());
//        GameProfile profile = new GameProfile(cloneUuid, name);
//        ClonePlayerEntity clone = new ClonePlayerEntity(server, world, profile, ownerUuid);
//
//
//        if (clone != null && profile != null) { // 最基礎的安全檢查
//            existingProfile.ifPresent(ownerProfile -> {
//                profile.getProperties().putAll(ownerProfile.getProperties());
//            });
//
//            clone.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
//            // --- 【關鍵移除】所有網路相關的程式碼都被刪除 ---
//            // 不再需要 ClientConnection 或 ServerPlayNetworkHandler
//
//            // 保持區塊載入 (這部分仍然需要，以確保實體不會被伺服器清理)
//            ChunkPos chunkPos = new ChunkPos(pos);
//            world.getChunkManager().addTicket(ChunkTicketType.FORCED, chunkPos, 2, chunkPos);
//
//            world.spawnEntity(clone); // ✅ 只使用 world.spawnEntity()，讓它作為一個普通世界實體存在。
//
//            System.out.println("Static model spawned successfully.");
//
//            return clone;
//        } else {
//            System.err.println("🚨 CRITICAL ERROR: Clone Entity setup failed due to missing profile or clone object.");
//            return clone; // 安全退出，防止 NPE
//        }
//
//    }
    public static ClonePlayerEntity spawnClone(MinecraftServer server, ServerWorld world, String name, UUID ownerUuid, BlockPos pos) {
        final ClonePlayerEntity[] clone = new ClonePlayerEntity[1];
        int delayTicks = 20;
        int targetTick = server.getTicks() + delayTicks;
// 使用一個簡單的 Tick 事件監聽
// 注意：這需要註冊一個臨時監聽器，或者在你的 Mod 的 Tick 事件中檢查
        ServerTickEvents.END_SERVER_TICK.register(new ServerTickEvents.EndTick() {
            private boolean executed = false;
            @Override
            public void onEndTick(MinecraftServer server) {
                if (executed) return;
                if (server.getTicks() >= targetTick) {
                    UUID cloneUuid = UUID.nameUUIDFromBytes(("Z_CLONE_ENTITY:" + ownerUuid.toString()).getBytes());
                    Entity existing = world.getEntity(cloneUuid);
                    if (existing != null) {
                        existing.discard(); // 或者 existing.remove(Entity.RemovalReason.DISCARDED);
                    }


                    Optional<GameProfile> existingProfile = server.getUserCache().getByUuid(ownerUuid);

                    GameProfile profile = new GameProfile(cloneUuid, name);

                    existingProfile.ifPresent(ownerProfile -> {
                        profile.getProperties().putAll(ownerProfile.getProperties());
                    });

                    clone[0] = new ClonePlayerEntity(server, world, profile, ownerUuid);
                    clone[0].refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);

                    // --- 關鍵修改 1：同步皮膚層次 ---
                    // 0x7F 表示開啟所有皮膚部分（外套、披肩、帽子等）
                    clone[0].getDataTracker().set(PlayerEntity.PLAYER_MODEL_PARTS, (byte) 0x7F);

                    // --- 網路處理 ---
                    ClientConnection fakeConnection = new ClientConnection(NetworkSide.SERVERBOUND);
                    clone[0].networkHandler = new ServerPlayNetworkHandler(server, fakeConnection, clone[0],
                            new ConnectedClientData(profile, 0, clone[0].getClientOptions(), false));

                    // --- 關鍵修改 2：封包發送順序 ---
                    // 1. 加入 Server PlayerList (這能處理後續進服玩家的同步)
                    server.getPlayerManager().getPlayerList().add(clone[0]);

                    // 2. 向當前在線玩家廣播：添加玩家資料（含皮膚）
                    // 注意：這裡使用的是 Action.ADD_PLAYER，確保客戶端收到 Profile
                    PlayerListS2CPacket addPacket = PlayerListS2CPacket.entryFromPlayer(List.of(clone[0]));
                    server.getPlayerManager().sendToAll(addPacket);

                    PlayerData playerData = Lucky_block_server_mod.DATA_MANAGER.getPlayerData(ownerUuid);

                    playerData.setClone_uuid(cloneUuid);

                    world.spawnEntity(clone[0]);
                    clone[0].interactionManager.changeGameMode(GameMode.CREATIVE);

                    // 移除 Tab 列表
                    //PlayerRemoveS2CPacket removePacket = new PlayerRemoveS2CPacket(List.of(clone[0].getUuid()));
                    //server.getPlayerManager().sendToAll(removePacket);
                    //System.out.println("Spawned NPC: " + name);
                    // 執行完後取消註冊，避免重複執行 (這部分邏輯需視你的框架而定)
                    executed = true;
                }
            }
        });

        //System.out.println("Spawned NPC: " + name);
        return clone[0];
    }

//        public static ClonePlayerEntity spawnClone(MinecraftServer server, ServerWorld world, String name, UUID ownerUuid, BlockPos pos) {
//            // 1. 嘗試從伺服器快取獲取完整的 GameProfile (包含皮膚數據)
//            // 如果玩家在線上，這通常能直接拿到包含 Skin 屬性的 Profile
//            Optional<GameProfile> existingProfile = server.getUserCache().getByUuid(ownerUuid);
//
//            UUID cloneUuid = UUID.nameUUIDFromBytes(("Z_CLONE_ENTITY:" + ownerUuid.toString()).getBytes());
//
//            GameProfile profile = new GameProfile(cloneUuid, name);
//
//            // 2. 複製皮膚屬性 (Properties)
//            existingProfile.ifPresent(ownerProfile -> {
//                profile.getProperties().putAll(ownerProfile.getProperties());
//            });
//
//            ClonePlayerEntity clone = new ClonePlayerEntity(server, world, profile, ownerUuid);
//            clone.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
//
//            // --- 網路處理部分 ---
//            ClientConnection fakeConnection = new ClientConnection(NetworkSide.SERVERBOUND);
//            clone.networkHandler = new ServerPlayNetworkHandler(server, fakeConnection, clone,
//                    new ConnectedClientData(profile, 0, clone.getClientOptions(), false));
//
//            // 保持區塊載入
//            ChunkPos chunkPos = new ChunkPos(pos);
//            world.getChunkManager().addTicket(ChunkTicketType.FORCED, chunkPos, 2, chunkPos);
//
//            // 加入 PlayerList 並生成
//            server.getPlayerManager().getPlayerList().add(clone);
//
//            PlayerListS2CPacket addPlayerPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, clone);
//            server.getPlayerManager().sendToAll(addPlayerPacket);
//
//            world.spawnEntity(clone);
//
//            System.out.println("spawnned");
//
//
//
//            return clone;
//
//    }


}