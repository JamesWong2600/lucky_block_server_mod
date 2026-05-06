package org.auto.lucky_block_server_mod.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

import static org.auto.lucky_block_server_mod.server_init.currentServer;


@Mixin(PalettedContainer.class)
public abstract class AntiChunkXrayMixin<T> {

    // 這是 1.21.4 序列化方塊的核心方法
    @Inject(method = "writePacket(Lnet/minecraft/network/PacketByteBuf;)V", at = @At("HEAD"))
    private void onWritePacket(PacketByteBuf buf, CallbackInfo ci) {
        // 注意：PalettedContainer 是一個通用的容器（也用於生物群系）
        // 我們必須確認它裡面裝的是 BlockState
    }
    @Inject(method = "get(III)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
    private void onGetBlock(int x, int y, int z, CallbackInfoReturnable<T> cir) {
        T value = cir.getReturnValue();
        if (value instanceof BlockState state) {
            if (state.isOf(Blocks.EMERALD_ORE) || state.isOf(Blocks.DEEPSLATE_EMERALD_ORE)) {
                // 暫時拿掉 isPacketContext()，強制全部變石頭
                //System.out.println("yes emerald");
                cir.setReturnValue((T) (Object) Blocks.STONE.getDefaultState());
            }
        }
    }
}