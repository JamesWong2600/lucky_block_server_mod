package org.auto.lucky_block_server_mod.lobby;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class lobby_gen {
    public static void generateGlassRoom(ServerWorld world) {
        int startX = -25;
        int startY = 0;
        int startZ = -25;
        int sizeX = 50;
        int sizeY = 20;
        int sizeZ = 50;

        BlockPos.Mutable pos = new BlockPos.Mutable();

        System.out.println("正在生成玻璃空間...");

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    int worldX = startX + x;
                    int worldY = startY + y;
                    int worldZ = startZ + z;

                    pos.set(worldX, worldY, worldZ);

                    // 邊界放置玻璃
                    if (x == 0 || x == sizeX - 1 ||
                            y == 0 || y == sizeY - 1 ||
                            z == 0 || z == sizeZ - 1) {
                        world.setBlockState(pos, Blocks.GLASS.getDefaultState());
                    } else {
                        // 內部填充空氣
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        // 放置地板（草地）
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                pos.set(worldX, startY - 1, worldZ);
                world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        // 放置光源（螢光石在天花板）
        for (int x = 4; x < sizeX - 4; x += 8) {
            for (int z = 4; z < sizeZ - 4; z += 8) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                pos.set(worldX, startY + sizeY - 2, worldZ);
                world.setBlockState(pos, Blocks.SEA_LANTERN.getDefaultState());
            }
        }

        System.out.println("玻璃空間生成完成！");
    }
}
