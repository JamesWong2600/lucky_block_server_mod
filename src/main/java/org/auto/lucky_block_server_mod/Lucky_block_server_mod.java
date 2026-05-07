package org.auto.lucky_block_server_mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import org.auto.lucky_block_server_mod.cache.DataMap;
import org.auto.lucky_block_server_mod.command.cinematic_manager;
import org.auto.lucky_block_server_mod.flow.countdown_timer;
import org.auto.lucky_block_server_mod.flow.game_timer;

import java.util.concurrent.CompletableFuture;

import static org.auto.lucky_block_server_mod.anti_xray.anti_xray.hideNonExposedOres;
import static org.auto.lucky_block_server_mod.cache.CooldownManager.checkTimeouts;
import static org.auto.lucky_block_server_mod.cache.DataMap.loadAllDataFromMongo;
import static org.auto.lucky_block_server_mod.cache.DataMap.statsMap;
import static org.auto.lucky_block_server_mod.command.startcommand.command_register;
import static org.auto.lucky_block_server_mod.flow.flow_controller.GetGameFlow;
import static org.auto.lucky_block_server_mod.lobby.lobby_gen.generateGlassRoom;
import static org.auto.lucky_block_server_mod.lobby.player_join_teleport_lobby.lobby_teleport_register;
import static org.auto.lucky_block_server_mod.performance_stat.NetWorkStuff.getCurrentMspt;
import static org.auto.lucky_block_server_mod.plyerdeadevent.playerdeadevent.death_to_specttor;
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
    @Override

    public void onInitialize() {
        DATA_MANAGER.initMongo("mongodb://admin:19431231BBwongwaihung@192.168.1.102:27017", "playerdataset", "playerdataset");
        loadAllDataFromMongo();
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
        death_to_specttor();
        //DATA_MANAGER.initMongo("mongodb://192.168.1.102:27017", "playerdataset", "playerdataset");

        // 2. 註冊定期 Flush (例如每 5 分鐘)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 每 20 tick (1秒) 檢查一次是否有玩家超過 60 秒沒連回
            if (server.getTicks() % 20 == 0) {
                if (GetGameFlow() == 2) {
                    // 檢查 CooldownManager 內的超時邏輯
                    checkTimeouts();
                }
            }

            // 你原本的每 5 秒數據 Flush
            if (server.getTicks() % 100 == 0) {
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
