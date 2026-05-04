package org.auto.lucky_block_server_mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import org.auto.lucky_block_server_mod.scoreboard.EventScoreboard;

import static org.auto.lucky_block_server_mod.Random_effect.applyRandomEffect;
import static org.auto.lucky_block_server_mod.lobby.lobby_gen.generateGlassRoom;

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
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == LOBBY_WORLD_KEY && !generated) {
                generateGlassRoom(world);
                generated = true;
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
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

    private void onServerTick(MinecraftServer server) {
        tickCounter++;

        // 每 20 Ticks (1秒) 更新一次計分板，避免造成網路擁堵
        if (tickCounter >= 20) {
            tickCounter = 0;

            // 獲取當前活動數據 (這裡假設你有個存放全域變數的地方)
            String currentStage = "準備時間";
            int timeLeft = 600;
            int borderSize = 600;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // 獲取玩家個人數據
                int broken = 12;
                String teamName = "第一組";

                // 更新顯示
                EventScoreboard.updateScoreboard(player, currentStage, timeLeft, broken, borderSize, teamName);
            }
        }
    }
}
