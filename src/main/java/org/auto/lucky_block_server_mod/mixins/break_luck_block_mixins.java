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
        if (world.isClient) return;

        BlockState state = this.world.getBlockState(pos);
        // 當一塊方塊被挖掉，檢查它周圍的 6 個方塊
        //for (Direction dir : Direction.values()) {
          //  BlockPos neighborPos = pos.offset(dir);
            //BlockState neighborState = world.getBlockState(neighborPos);

            // 如果鄰居是礦石，我們需要重新發送一個「真實」的封包
//            if (isOre(neighborState)) {
//                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
//                // 發送真實的方塊狀態，覆蓋掉之前 Mixin 產生的假石頭
//                serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(neighborPos, neighborState));
//            }
   //     }
        updateClientBlocksAroundPlayer(player, pos);
        if (state.isOf(Blocks.EMERALD_ORE)) {
            // 調用外部數據類，不再報錯
            LuckBlockData.addCount(player.getUuid());

            applyRandomEffect(player);
            this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            cir.setReturnValue(true);
        }
    }

    private static final Set<Block> HIDDEN_BLOCKS = Set.of(
            // 鑽石
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            // 綠寶石 (幸運方塊通常用這個)
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            // 金礦
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
            // 鐵礦
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            // 銅礦
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            // 煤礦
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            // 紅石
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            // 青金石
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            // 下界特有
            Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS // 古骸 (獄髓原礦)
    );

    private boolean isOre(BlockState state) {
        return HIDDEN_BLOCKS.contains(state.getBlock());
    }


    // 提供一個公開方法讓你的 Scoreboard 獲取數值
}