package org.auto.lucky_block_server_mod.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.auto.lucky_block_server_mod.lucky_block_data.LuckBlockData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.auto.lucky_block_server_mod.Random_effect.applyRandomEffect;

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

        if (state.isOf(Blocks.EMERALD_ORE)) {
            // 調用外部數據類，不再報錯
            LuckBlockData.addCount(player.getUuid());

            applyRandomEffect(player);
            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            cir.setReturnValue(true);
        }
    }

    // 提供一個公開方法讓你的 Scoreboard 獲取數值
}