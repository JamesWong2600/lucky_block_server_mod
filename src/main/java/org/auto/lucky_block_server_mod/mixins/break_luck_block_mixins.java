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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.auto.lucky_block_server_mod.Random_effect.applyRandomEffect;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class break_luck_block_mixins {

    @Shadow @Final protected ServerPlayerEntity player;
    @Shadow protected ServerWorld world;

    @Inject(
            method = "tryBreakBlock", // 在 Yarn 中，destroyBlock 通常對應為 tryBreakBlock
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/block/BlockState;"
            ),
            cancellable = true
    )
    private void onLuckyBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = this.world.getBlockState(pos);

        if (state.isOf(Blocks.EMERALD_ORE)) {
            // 執行你的幸運方塊邏輯
            // YourMod.triggerLuckyEffect(this.player, pos);
            applyRandomEffect(player);
            // 移除方塊並取消原版掉落
            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

            // 讓方法回傳 true 並終止後續邏輯
            cir.setReturnValue(true);
        }
    }
}