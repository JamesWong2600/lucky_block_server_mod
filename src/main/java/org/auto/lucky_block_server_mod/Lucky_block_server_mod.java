package org.auto.lucky_block_server_mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import org.auto.lucky_block_server_mod.command.cinematic_manager;
import org.auto.lucky_block_server_mod.flow.countdown_timer;
import org.auto.lucky_block_server_mod.flow.game_timer;

import static org.auto.lucky_block_server_mod.command.startcommand.command_register;
import static org.auto.lucky_block_server_mod.lobby.lobby_gen.generateGlassRoom;
import static org.auto.lucky_block_server_mod.lobby.player_join_teleport_lobby.lobby_teleport_register;
import static org.auto.lucky_block_server_mod.scoreboard.tick_scoreboard_handler.timer_register;

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

    @Override
    public void onInitialize() {
        lobby_teleport_register();
        command_register();
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == LOBBY_WORLD_KEY && !generated) {
                generateGlassRoom(world);
                generated = true;
            }
            ServerWorld overworld = server.getWorld(World.OVERWORLD);

            if (overworld != null) {
                // 2. 獲取該世界的邊界
                var border = overworld.getWorldBorder();

                // 3. 進行設置
                border.setCenter(0, 0);
                border.setSize(1600.0);

                System.out.println("成功設置 Overworld 邊界");
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            cinematic_manager.tick(server);
            countdown_timer.tick(server);
            game_timer.game_tick(server);
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
