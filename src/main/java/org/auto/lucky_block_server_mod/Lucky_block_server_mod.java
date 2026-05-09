package org.auto.lucky_block_server_mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import org.auto.lucky_block_server_mod.cache.DataMap;
import org.auto.lucky_block_server_mod.cache.PlayerSpawnTask;
import org.auto.lucky_block_server_mod.cache.ServerInfo;
import org.auto.lucky_block_server_mod.clone_player_entity.ClonePlayerEntity;
import org.auto.lucky_block_server_mod.command.cinematic_manager;
import org.auto.lucky_block_server_mod.data.player_amount;
import org.auto.lucky_block_server_mod.flow.countdown_timer;
import org.auto.lucky_block_server_mod.flow.game_timer;

import java.nio.file.Path;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.auto.lucky_block_server_mod.anti_xray.anti_xray_air_wall.updateClientBlocksAroundPlayer;
import static org.auto.lucky_block_server_mod.cache.CooldownManager.checkTimeouts;
import static org.auto.lucky_block_server_mod.cache.DataMap.loadAllDataFromMongo;
import static org.auto.lucky_block_server_mod.cache.DataMap.statsMap;
import static org.auto.lucky_block_server_mod.command.startcommand.command_register;
import static org.auto.lucky_block_server_mod.flow.flow_controller.GetGameFlow;
import static org.auto.lucky_block_server_mod.flow.game_timer.getTotalSeconds;
import static org.auto.lucky_block_server_mod.lobby.lobby_gen.generateGlassRoom;
import static org.auto.lucky_block_server_mod.lobby.player_join_teleport_lobby.lobby_teleport_register;
import static org.auto.lucky_block_server_mod.performance_stat.NetWorkStuff.getCurrentMspt;
import static org.auto.lucky_block_server_mod.scoreboard.tick_scoreboard_handler.timer_register;
import static org.auto.lucky_block_server_mod.server_init.server_initer;

public class Lucky_block_server_mod implements ModInitializer {
    public static final RegistryKey<DimensionOptions> LOBBY_DIMENSION_KEY = RegistryKey.of(
            RegistryKeys.DIMENSION,
            Identifier.of("lobby","lobby")
    );

    // 註冊世界 Key
    public static final RegistryKey<World> LOBBY_WORLD_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("lobby","lobby")
    );

    private boolean generated = false;
    private int tickCounter = 0;
    public static double currentMspt = -1;

    public static final DataMap DATA_MANAGER = new DataMap();
    public static ServerInfo serverManager = new ServerInfo();
    public static final Queue<PlayerSpawnTask> spawnQueue = new ConcurrentLinkedQueue<>();

    @Override

    public void onInitialize() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient()) {
                updateClientBlocksAroundPlayer(player, pos);
            }
            return true;
        });
        // 初始化 (傳入路徑與你的 DataMap 實例)
        player_amount.init(configDir, Lucky_block_server_mod.DATA_MANAGER);
        loadAllDataFromMongo();

        serverManager.setGroup(4); // 例如，設定起始分組數為 4

        // 🌟 新增：將初始化時的伺服器狀態立即寫入到 MongoDB
        DATA_MANAGER.flushServerInfoToMongo(serverManager);
        // 2. 等待 MongoDB 連接建立（可選，確保連接成功）
//        try {
//            //Thread.sleep(100);
//            System.out.println("✓ MongoDB is ready and connected");// 給 MongoDB 一點時間建立連接
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


        lobby_teleport_register();
        command_register();
        server_initer();
        //death_to_specttor();
        //DATA_MANAGER.initMongo("mongodb://192.168.1.102:27017", "playerdataset", "playerdataset");

        // 2. 註冊定期 Flush (例如每 5 分鐘)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getGameRules().get(GameRules.DO_IMMEDIATE_RESPAWN).set(true, server);

            while (!spawnQueue.isEmpty()) {
                PlayerSpawnTask task = spawnQueue.poll(); // 從隊列取出第一個任務

                if (task != null) {

                    UUID uuid = task.getPlayerUuid();
                    int radius = task.getRadius();

                    ServerPlayerEntity playerToSpawnFor = server.getPlayerManager().getPlayer(uuid);

                    if (playerToSpawnFor != null) {
                        // 將 Getter 取得的值傳入 spawnAtRandomTopPos
                        ClonePlayerEntity.spawnAtRandomTopPos(playerToSpawnFor, radius);
                    } else {
                        System.out.println("⚠️ [Spawn Task] Player " + uuid + " logged out while waiting for spawn task.");
                    }
                }
            }
            ServerInfo currentServerInfo = Lucky_block_server_mod.serverManager;
            // 每 20 tick (1秒) 檢查一次是否有玩家超過 60 秒沒連回
            if (server.getTicks() % 20 == 0) {
                if(getTotalSeconds() > 100){
                    currentServerInfo.setSession(3);
                }
                if (currentServerInfo.getSession() == 3) {
                    ServerWorld overworld = server.getWorld(World.OVERWORLD);

                    if (overworld != null) {
                        // 2. 獲取該世界的邊界
                        var border = overworld.getWorldBorder();

                        // 3. 進行設置
                        border.setCenter(0, 0);
                        border.interpolateSize(1600L, 10L, 1200);

                        System.out.println("開始縮圈");
                    }
                }
                System.out.println(currentServerInfo.getSession());
                if (currentServerInfo.getSession() == 2) {
                    // 檢查 CooldownManager 內的超時邏輯
                    player_amount.updateRedisCount(server);
                    checkTimeouts();
                }
            }

            // 你原本的每 5 秒數據 Flush
            if (server.getTicks() % 100 == 0) {
                System.out.println("\n[TASK] Running Maintenance Task...");

                // A. 更新全局狀態，例如遊戲運行時間增加 5 秒
                long currentTime = System.currentTimeMillis();
                long newTimeRunned = serverManager.getTimeRunned() + 5000;
                serverManager.setTimeRunned(newTimeRunned);

                // B. **【核心整合】** 將 ServerInfo 的最新狀態寫入到 'serverdata' collection
                DATA_MANAGER.flushServerInfoToMongo(serverManager);
                System.out.println("[TASK] Global server state saved.");

                System.out.println(statsMap);
                System.out.println(GetGameFlow());
                CompletableFuture.runAsync(DATA_MANAGER::flushToMongo);
            }
        });
//        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
//            if (!world.isClient()) {
//                hideNonExposedOres(world, chunk);
//            }
//        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerWorld overworld = server.getWorld(World.OVERWORLD);

            if (overworld != null) {
                // 2. 獲取該世界的邊界
                var border = overworld.getWorldBorder();

                // 3. 進行設置
                border.setCenter(0, 0);
                border.setMaxRadius(1600);

                System.out.println("SUCESS BOARDER");
            }
        });
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == LOBBY_WORLD_KEY && !generated) {
                generateGlassRoom(world);
                generated = true;
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            cinematic_manager.tick(server);
            countdown_timer.tick(server);
            game_timer.game_tick(server);
            currentMspt = getCurrentMspt(server);
         });
//        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        timer_register();
//        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
//            // 檢查是否為綠寶石原礦
//            if (state.isOf(Blocks.EMERALD_ORE)) {
//                // 取消掉落物並填充為空氣
//                world.setBlockState(pos, Blocks.AIR.getDefaultState());
//
//                // 觸發隨機效果
//                if (player instanceof ServerPlayerEntity serverPlayer) {
//                    applyRandomEffect(serverPlayer);
//                }
//            }
//        });
    }

//    private void onServerTick(MinecraftServer server) {
//        tickCounter++;
//
//        // 每 20 Ticks (1秒) 更新一次計分板，避免造成網路擁堵
//        if (tickCounter >= 20) {
//            tickCounter = 0;
//
//            // 獲取當前活動數據 (這裡假設你有個存放全域變數的地方)
//            String currentStage = "準備時間";
//            int timeLeft = 600;
//            int borderSize = 600;
//
//            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
//                // 獲取玩家個人數據
//                int broken = 12;
//                String teamName = "第一組";
//
//                // 更新顯示
//                EventScoreboard.updateScoreboard(player, currentStage, timeLeft, broken, borderSize, teamName);
//            }
//        }
//    }
}
