package org.auto.lucky_block_server_mod.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.auto.lucky_block_server_mod.Lucky_block_server_mod;
import org.auto.lucky_block_server_mod.cache.PlayerData;
import org.auto.lucky_block_server_mod.cache.ServerInfo;
import org.auto.lucky_block_server_mod.lucky_block_data.LuckBlockData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.auto.lucky_block_server_mod.Random_effect.applyRandomEffect;
import static org.auto.lucky_block_server_mod.anti_xray.anti_xray_air_wall.updateClientBlocksAroundPlayer;
import static org.auto.lucky_block_server_mod.flow.flow_controller.GetGameFlow;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class break_luck_block_mixins {

    @Shadow @Final protected ServerPlayerEntity player;
    @Shadow protected ServerWorld world;

    @Inject(
            method = "tryBreakBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/block/BlockState;"
            ),
            cancellable = true
    )
    private void onLuckyBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = this.world.getBlockState(pos);

        if (this.player.hasPermissionLevel(4)) {
            return;
        }

        ServerInfo currentServerInfo = Lucky_block_server_mod.serverManager;

        if (currentServerInfo.getSession() == 2 && state.isOf(Blocks.EMERALD_ORE)) {

            PlayerData playerData = Lucky_block_server_mod.DATA_MANAGER.getPlayerData(player.getUuid());

            playerData.incrementBlockBreak();
            // 執行你的幸運方塊邏輯
            // YourMod.triggerLuckyEffect(this.player, pos);
            applyRandomEffect(player);
            // 移除方塊並取消原版掉落
            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

            updateClientBlocksAroundPlayer(player, pos);

            // 讓方法回傳 true 並終止後續邏輯
            cir.setReturnValue(true);
        }
    }
//    private void onLuckyBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
//        if (world.isClient) return;
//
//        // 1. 如果是 OP，直接跳過幸運方塊邏輯（不計數、不給效果）
//        if (this.player.hasPermissionLevel(4)) {
//            return;
//        }
//
//        BlockState state = this.world.getBlockState(pos);
//
//        // 2. 只有在 Flow = 2 (比賽中) 且挖掘的是綠寶石礦時才觸發
//        // 這裡假設 GetGameFlow() 是全域可存取的
//        if (GetGameFlow() == 2 && state.isOf(Blocks.EMERALD_ORE)) {
//
//            // 重要：直接調用你主類別中的 DATA_MANAGER
//            // 這樣數據才會進到你那個 ConcurrentHashMap，進而 Flush 到 MongoDB
//            Lucky_block_server_mod.DATA_MANAGER.addBlockBreak(player.getUuid());
//
//            // 執行特效
//            applyRandomEffect(player);
//
//            // 周圍方塊更新 (處理反透視假礦)
//            updateClientBlocksAroundPlayer(player, pos);
//
//            // 移除方塊並取消原始邏輯
//            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
//            cir.setReturnValue(false);
//        }

}
//        updateClientBlocksAroundPlayer(player, pos);
//        if (state.isOf(Blocks.EMERALD_ORE)) {
//            // 調用外部數據類，不再報錯
//            LuckBlockData.addCount(player.getUuid());
//
//            applyRandomEffect(player);
//            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
//            cir.setReturnValue(true);
//        }
//    }
//
//    private static final Set<Block> HIDDEN_BLOCKS = Set.of(
//            // 鑽石
//            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
//            // 綠寶石 (幸運方塊通常用這個)
//            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
//            // 金礦
//            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
//            // 鐵礦
//            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
//            // 銅礦
//            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
//            // 煤礦
//            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
//            // 紅石
//            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
//            // 青金石
//            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
//            // 下界特有
//            Blocks.NETHER_QUARTZ_ORE,
//            Blocks.ANCIENT_DEBRIS // 古骸 (獄髓原礦)
//    );
//
//    private boolean isOre(BlockState state) {
//        return HIDDEN_BLOCKS.contains(state.getBlock());
//    }


    // 提供一個公開方法讓你的 Scoreboard 獲取數值
