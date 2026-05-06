package org.auto.lucky_block_server_mod.anti_xray;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;

import static net.minecraft.block.Blocks.DEEPSLATE;
import static net.minecraft.block.Blocks.STONE;
import static net.minecraft.util.shape.VoxelShapes.fullCube;
import static org.auto.lucky_block_server_mod.server_init.currentServer;

public class anti_xray_air_wall {
    public static void updateClientBlocksAroundPlayer(PlayerEntity players, BlockPos pos_2){
              if (players.getUuid() == null) return;
            ServerPlayerEntity player = currentServer.getPlayerManager().getPlayer(players.getUuid());
            if (player == null) return;
            if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) return;

            int totalRadius = 3;

            // 玩家站立的高度有兩個方塊：腳下和頭部位置
            // 玩家腳下位置的y座標是 center.getY()
            // 玩家頭部位置的y座標是 center.getY() + 1

            // 處理整個範圍
            for (int dx = -totalRadius; dx <= totalRadius; dx++) {
                for (int dy = -totalRadius; dy <= totalRadius; dy++) {
                    for (int dz = -totalRadius; dz <= totalRadius; dz++) {
                        BlockPos pos = pos_2.add(dx, dy, dz);

                        // 檢查是否與玩家碰撞箱直接接觸
                        // 玩家碰撞箱約為 0.6寬 × 1.8高 × 0.6深

                        // 定義玩家腳部和頭部的方塊位置
                        boolean isPlayerFootBlock = (dx == 0 && dy == 0 && dz == 0);      // 腳下方塊
                        boolean isPlayerHeadBlock = (dx == 0 && dy == 1 && dz == 0);      // 頭部方塊

                        // 玩家腳部周圍的8個直接接觸方塊
                        boolean isFootLevelContact =
                                (dy == 0 && (
                                        // 腳部水平方向4個面
                                        (dx == 0 && Math.abs(dz) == 1) ||    // 前/後
                                                (dz == 0 && Math.abs(dx) == 1)       // 左/右
                                ));

                        // 玩家頭部周圍的8個直接接觸方塊
                        boolean isHeadLevelContact =
                                (dy == 1 && (
                                        // 頭部水平方向4個面
                                        (dx == 0 && Math.abs(dz) == 1) ||    // 前/後
                                                (dz == 0 && Math.abs(dx) == 1)       // 左/右
                                ));

                        // 玩家垂直方向的接觸（上方2個，下方2個）
                        boolean isVerticalContact =
                                (dx == 0 && dz == 0 && (
                                        (dy == 2) ||      // 頭頂上方
                                                (dy == -1)        // 腳下下方
                                ));

                        // 總共應該是：
                        // 1. 腳下方塊下方 (-1) = 1個
                        // 2. 腳下方塊周圍4個面 = 4個
                        // 3. 頭部方塊周圍4個面 = 4個
                        // 4. 頭部方塊上方 (+2) = 1個
                        // 總計：10個方塊

                        // 計算距離的簡化版本
                        boolean isDirectContact = false;

                        // 方法1：使用曼哈頓距離，只保留距離為1的方塊
                        int manhattanDistance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);

                        // 考慮玩家高度，我們需要檢查兩個高度層
                        for (int playerY = 0; playerY <= 1; playerY++) {
                            int relY = dy - playerY;  // 相對於玩家該身體部位的高度

                            // 直接接觸的條件：曼哈頓距離為1，且不在玩家佔據的方塊內
                            if (manhattanDistance == 1 &&
                                    !(dx == 0 && dy == playerY && dz == 0)) {
                                isDirectContact = true;
                                break;
                            }

                            // 另外考慮正上方和正下方
                            if (dx == 0 && dz == 0 &&
                                    (relY == 1 || relY == -1)) {
                                isDirectContact = true;
                                break;
                            }
                        }

                        // 額外檢查：玩家站立的下方方塊（不是腳下，是腳下再下面一格）
                        if (dx == 0 && dy == -1 && dz == 0) {
                            isDirectContact = true;
                        }

                        if (isDirectContact) {
                            // 直接接觸的方塊發送真實數據
                            BlockState realState = player.getWorld().getBlockState(pos);
                            player.networkHandler.sendPacket(
                                    new BlockUpdateS2CPacket(pos, realState)
                            );
                        } else {
                            // 其他所有方塊發送空氣
                            if (shouldShowAsAir(player.getServerWorld(), pos)) {
                                player.networkHandler.sendPacket(
                                        new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState())
                                );
                            }
                        }
                    }
                }
            }
    }


    private static boolean shouldShowAsAir(ServerWorld world, BlockPos pos) {
        BlockState currentState = world.getBlockState(pos);

        // 只對特定方塊類型顯示為空氣
        if (!currentState.isOf(STONE) &&
                !currentState.isOf(DEEPSLATE) &&
                !currentState.isOf(Blocks.ANDESITE) &&
                !currentState.isOf(Blocks.DIORITE) &&
                !currentState.isOf(Blocks.GRANITE) &&
                !currentState.isOf(Blocks.TUFF) &&
                !currentState.isOf(Blocks.CALCITE) &&
                !currentState.isOf(Blocks.DIAMOND_ORE) &&
                !currentState.isOf(Blocks.EMERALD_ORE) &&
                !currentState.isOf(Blocks.GOLD_ORE) &&
                !currentState.isOf(Blocks.REDSTONE_ORE) &&
                !currentState.isOf(Blocks.LAPIS_ORE) &&
                !currentState.isOf(Blocks.IRON_ORE) &&
                !currentState.isOf(Blocks.COPPER_ORE) &&
                !currentState.isOf(Blocks.COAL_ORE) &&
                !currentState.isOf(Blocks.DEEPSLATE_DIAMOND_ORE) &&
                !currentState.isOf(Blocks.DEEPSLATE_EMERALD_ORE) &&
                !currentState.isOf(Blocks.DEEPSLATE_GOLD_ORE) &&
                !currentState.isOf(Blocks.DEEPSLATE_IRON_ORE) &&
                !currentState.isOf(Blocks.DEEPSLATE_COPPER_ORE) &&
                !currentState.isOf(Blocks.DEEPSLATE_COAL_ORE) &&
                !currentState.isOf(Blocks.DEEPSLATE_LAPIS_ORE) &&
                !currentState.isOf(Blocks.DEEPSLATE_REDSTONE_ORE)){
            return false;
        }

        // 檢查是否暴露在空氣中
        return !isExposedToAir(world, pos);
    }
    private static boolean isFullBlock(BlockState state) {
        // 检查是否为完整方块

        return state.getCullingShape().equals(fullCube());
    }

    private static boolean isExposedToAir(ServerWorld world, BlockPos pos) {
        // 檢查六個方向
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            if (neighborBlock == Blocks.AIR ||
                    neighborBlock == Blocks.CAVE_AIR ||
                    neighborBlock == Blocks.VOID_AIR ||
                    neighborBlock == Blocks.WATER ||
                    neighborBlock == Blocks.LAVA ||
                    !isFullBlock(neighborState)) {     // 非固體方塊
                return true;
            }
        }
        return false;
    }
}
