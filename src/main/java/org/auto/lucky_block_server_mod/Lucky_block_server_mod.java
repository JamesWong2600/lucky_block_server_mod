package org.auto.lucky_block_server_mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;

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

    @Override
    public void onInitialize() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == LOBBY_WORLD_KEY && !generated) {
                generateGlassRoom(world);
                generated = true;
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            // 檢查是否為綠寶石原礦
            if (state.isOf(Blocks.EMERALD_ORE)) {
                // 取消掉落物並填充為空氣
                world.setBlockState(pos, Blocks.AIR.getDefaultState());

                // 觸發隨機效果
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    applyRandomEffect(serverPlayer);
                }
            }
        });
    }
}
