package org.auto.lucky_block_server_mod.anti_xray;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

public class anti_xray {
    public static void hideNonExposedOres(ServerWorld world, WorldChunk chunk) {
        int hidden = 0;

        // 遍歷區塊內所有方塊
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getBottomY(); y <= world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z); y++) {
                    BlockPos pos = new BlockPos(
                            chunk.getPos().getStartX() + x,
                            y,
                            chunk.getPos().getStartZ() + z
                    );

                    // 如果是綠寶石礦
                    if (chunk.getBlockState(pos).isOf(Blocks.EMERALD_ORE)) {
                        // 檢查是否曝露於空氣
                        if (!isExposedToAir(world, pos)) {
                            // 不曝露 → 改成石頭
                            chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                            hidden++;
                        }
                    }
                }
            }
        }

        if (hidden > 0) {
            System.out.println("[AntiXray] 區塊 " + chunk.getPos().x + "," + chunk.getPos().z
                    + " 隱藏了 " + hidden + " 個綠寶石礦");
        }
    }

    private static boolean isExposedToAir(ServerWorld world, BlockPos pos) {
        // 檢查六個方向
        return world.getBlockState(pos.up()).isAir() ||
                world.getBlockState(pos.down()).isAir() ||
                world.getBlockState(pos.north()).isAir() ||
                world.getBlockState(pos.south()).isAir() ||
                world.getBlockState(pos.east()).isAir() ||
                world.getBlockState(pos.west()).isAir();
    }
}
