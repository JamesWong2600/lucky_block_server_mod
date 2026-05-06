package org.auto.lucky_block_server_mod.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.block.Blocks.DEEPSLATE;
import static net.minecraft.block.Blocks.STONE;
import static net.minecraft.util.shape.VoxelShapes.fullCube;

@Mixin(MinecraftServer.class)
public class AntiXrayMixin {
    private int tickCounter = 0;

//    @Inject(method = "tick", at = @At("TAIL"))
//    private void onServerTick(CallbackInfo ci) {
//        tickCounter++;
//        if (tickCounter % 10 != 0) return;
//
//        for (ServerWorld world : ((MinecraftServer)(Object)this).getWorlds()) {
//            for (ServerPlayerEntity player : world.getPlayers()) {
//                if (player.getGameMode() == GameMode.SPECTATOR) continue;
//
//                BlockPos center = player.getBlockPos();
//                int totalRadius = 3;
//
//                // 玩家站立的高度有兩個方塊：腳下和頭部位置
//                // 玩家腳下位置的y座標是 center.getY()
//                // 玩家頭部位置的y座標是 center.getY() + 1
//
//                // 處理整個範圍
//                for (int dx = -totalRadius; dx <= totalRadius; dx++) {
//                    for (int dy = -totalRadius; dy <= totalRadius; dy++) {
//                        for (int dz = -totalRadius; dz <= totalRadius; dz++) {
//                            BlockPos pos = center.add(dx, dy, dz);
//
//                            // 檢查是否與玩家碰撞箱直接接觸
//                            // 玩家碰撞箱約為 0.6寬 × 1.8高 × 0.6深
//
//                            // 定義玩家腳部和頭部的方塊位置
//                            boolean isPlayerFootBlock = (dx == 0 && dy == 0 && dz == 0);      // 腳下方塊
//                            boolean isPlayerHeadBlock = (dx == 0 && dy == 1 && dz == 0);      // 頭部方塊
//
//                            // 玩家腳部周圍的8個直接接觸方塊
//                            boolean isFootLevelContact =
//                                    (dy == 0 && (
//                                            // 腳部水平方向4個面
//                                            (dx == 0 && Math.abs(dz) == 1) ||    // 前/後
//                                                    (dz == 0 && Math.abs(dx) == 1)       // 左/右
//                                    ));
//
//                            // 玩家頭部周圍的8個直接接觸方塊
//                            boolean isHeadLevelContact =
//                                    (dy == 1 && (
//                                            // 頭部水平方向4個面
//                                            (dx == 0 && Math.abs(dz) == 1) ||    // 前/後
//                                                    (dz == 0 && Math.abs(dx) == 1)       // 左/右
//                                    ));
//
//                            // 玩家垂直方向的接觸（上方2個，下方2個）
//                            boolean isVerticalContact =
//                                    (dx == 0 && dz == 0 && (
//                                            (dy == 2) ||      // 頭頂上方
//                                                    (dy == -1)        // 腳下下方
//                                    ));
//
//                            // 總共應該是：
//                            // 1. 腳下方塊下方 (-1) = 1個
//                            // 2. 腳下方塊周圍4個面 = 4個
//                            // 3. 頭部方塊周圍4個面 = 4個
//                            // 4. 頭部方塊上方 (+2) = 1個
//                            // 總計：10個方塊
//
//                            // 計算距離的簡化版本
//                            boolean isDirectContact = false;
//
//                            // 方法1：使用曼哈頓距離，只保留距離為1的方塊
//                            int manhattanDistance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
//
//                            // 考慮玩家高度，我們需要檢查兩個高度層
//                            for (int playerY = 0; playerY <= 1; playerY++) {
//                                int relY = dy - playerY;  // 相對於玩家該身體部位的高度
//
//                                // 直接接觸的條件：曼哈頓距離為1，且不在玩家佔據的方塊內
//                                if (manhattanDistance == 1 &&
//                                        !(dx == 0 && dy == playerY && dz == 0)) {
//                                    isDirectContact = true;
//                                    break;
//                                }
//
//                                // 另外考慮正上方和正下方
//                                if (dx == 0 && dz == 0 &&
//                                        (relY == 1 || relY == -1)) {
//                                    isDirectContact = true;
//                                    break;
//                                }
//                            }
//
//                            // 額外檢查：玩家站立的下方方塊（不是腳下，是腳下再下面一格）
//                            if (dx == 0 && dy == -1 && dz == 0) {
//                                isDirectContact = true;
//                            }
//
//                            if (isDirectContact) {
//                                // 直接接觸的方塊發送真實數據
//                                BlockState realState = player.getWorld().getBlockState(pos);
//                                player.networkHandler.sendPacket(
//                                        new BlockUpdateS2CPacket(pos, realState)
//                                );
//                            } else {
//                                // 其他所有方塊發送空氣
//                                if (shouldShowAsAir(player.getServerWorld(), pos)) {
//                                    player.networkHandler.sendPacket(
//                                            new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState())
//                                    );
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
@Inject(method = "tick", at = @At("TAIL"))
private void onServerTick(CallbackInfo ci) {
    tickCounter++;
    if (tickCounter % 10 != 0) return;

    for (ServerWorld world : ((MinecraftServer)(Object)this).getWorlds()) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) continue;

            BlockPos center = player.getBlockPos();
            int totalRadius = 3;

            // 玩家站立的高度有兩個方塊：腳下和頭部位置
            // 玩家腳下位置的y座標是 center.getY()
            // 玩家頭部位置的y座標是 center.getY() + 1

            // 處理整個範圍
            for (int dx = -totalRadius; dx <= totalRadius; dx++) {
                for (int dy = -totalRadius; dy <= totalRadius; dy++) {
                    for (int dz = -totalRadius; dz <= totalRadius; dz++) {
                        BlockPos pos = center.add(dx, dy, dz);

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
    }
}

//    @Inject(method = "tick", at = @At("TAIL"))
//    private void onServerTick(CallbackInfo ci) {
//        tickCounter++;
//        if (tickCounter % 10 != 0) return;
//
//        for (ServerWorld world : ((MinecraftServer)(Object)this).getWorlds()) {
//            for (ServerPlayerEntity player : world.getPlayers()) {
//                if (player.getGameMode() == GameMode.SPECTATOR) continue;
//
//                BlockPos center = player.getBlockPos();
//                int totalRadius = 3;
//
//                // 獲取玩家準心看向的方向
//                Vec3d lookVec = player.getRotationVec(1.0F);
//
//                // 簡化：根據俯仰角判斷上下，根據偏航角判斷水平方向
//                float pitch = player.getPitch();
//                float yaw = player.getYaw();
//
//                // 判斷主要朝向
//                Direction primaryDirection;
//                if (pitch > 45) {
//                    primaryDirection = Direction.DOWN;
//                } else if (pitch < -45) {
//                    primaryDirection = Direction.UP;
//                } else {
//                    // 水平方向，使用偏航角
//                    primaryDirection = Direction.getFacing(lookVec);
//                }
//
//                // 處理整個範圍
//                for (int dx = -totalRadius; dx <= totalRadius; dx++) {
//                    for (int dy = -totalRadius; dy <= totalRadius; dy++) {
//                        for (int dz = -totalRadius; dz <= totalRadius; dz++) {
//                            BlockPos pos = center.add(dx, dy, dz);
//
//                            // 計算是否為直接接觸方塊
//                            boolean isDirectContact = calculateDirectContact(dx, dy, dz);
//
//                            // 檢查是否在朝向的邊緣（菱角）
//                            boolean isOnFacingEdge = isOnFacingEdge(dx, dy, dz, primaryDirection, totalRadius);
//
//                            // 檢查是否在朝向的角（corner，更精確的菱角）
//                            boolean isOnFacingCorner = isOnFacingCorner(dx, dy, dz, primaryDirection, totalRadius);
//
//                            // 檢查是否在朝向面邊緣
//                            boolean isOnFacingFaceEdge = isOnFacingFaceEdge(dx, dy, dz, primaryDirection, totalRadius);
//
//                            if (isDirectContact || isOnFacingCorner || isOnFacingFaceEdge) {
//                                // 發送真實數據
//                                BlockState realState = player.getWorld().getBlockState(pos);
//                                player.networkHandler.sendPacket(
//                                        new BlockUpdateS2CPacket(pos, realState)
//                                );
//                            } else {
//                                // 其他所有方塊發送空氣
//                                if (shouldShowAsAir(player.getServerWorld(), pos)) {
//                                    player.networkHandler.sendPacket(
//                                            new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState())
//                                    );
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    // 判斷是否在朝向的邊緣（整個面）
    private boolean isOnFacingEdge(int dx, int dy, int dz, Direction facing, int radius) {
        switch (facing) {
            case NORTH:
                return dz == -radius;
            case SOUTH:
                return dz == radius;
            case WEST:
                return dx == -radius;
            case EAST:
                return dx == radius;
            case UP:
                return dy == radius;
            case DOWN:
                return dy == -radius;
            default:
                return false;
        }
    }

    // 判斷是否在朝向的角（真正的菱角）
    private boolean isOnFacingCorner(int dx, int dy, int dz, Direction facing, int radius) {
        switch (facing) {
            case NORTH: // 朝北，檢查北面的四個角
                if (dz == -radius) {
                    // 北面的四個角：(±radius, ±radius, -radius)
                    return (dx == -radius && dy == -radius) ||
                            (dx == -radius && dy == radius) ||
                            (dx == radius && dy == -radius) ||
                            (dx == radius && dy == radius);
                }
                break;
            case SOUTH: // 朝南，檢查南面的四個角
                if (dz == radius) {
                    return (dx == -radius && dy == -radius) ||
                            (dx == -radius && dy == radius) ||
                            (dx == radius && dy == -radius) ||
                            (dx == radius && dy == radius);
                }
                break;
            case WEST: // 朝西，檢查西面的四個角
                if (dx == -radius) {
                    return (dz == -radius && dy == -radius) ||
                            (dz == -radius && dy == radius) ||
                            (dz == radius && dy == -radius) ||
                            (dz == radius && dy == radius);
                }
                break;
            case EAST: // 朝東，檢查東面的四個角
                if (dx == radius) {
                    return (dz == -radius && dy == -radius) ||
                            (dz == -radius && dy == radius) ||
                            (dz == radius && dy == -radius) ||
                            (dz == radius && dy == radius);
                }
                break;
            case UP: // 朝上，檢查上面的四個角
                if (dy == radius) {
                    return (dx == -radius && dz == -radius) ||
                            (dx == -radius && dz == radius) ||
                            (dx == radius && dz == -radius) ||
                            (dx == radius && dz == radius);
                }
                break;
            case DOWN: // 朝下，檢查下面的四個角
                if (dy == -radius) {
                    return (dx == -radius && dz == -radius) ||
                            (dx == -radius && dz == radius) ||
                            (dx == radius && dz == -radius) ||
                            (dx == radius && dz == radius);
                }
                break;
        }
        return false;
    }

    // 判斷是否在朝向面的邊緣（不包括角）
    private boolean isOnFacingFaceEdge(int dx, int dy, int dz, Direction facing, int radius) {
        switch (facing) {
            case NORTH:
                if (dz == -radius) {
                    // 北面的邊緣（不包括角）
                    return (dx >= -radius && dx <= radius && dy >= -radius && dy <= radius) &&
                            !((dx == -radius || dx == radius) && (dy == -radius || dy == radius));
                }
                break;
            case SOUTH:
                if (dz == radius) {
                    return (dx >= -radius && dx <= radius && dy >= -radius && dy <= radius) &&
                            !((dx == -radius || dx == radius) && (dy == -radius || dy == radius));
                }
                break;
            case WEST:
                if (dx == -radius) {
                    return (dz >= -radius && dz <= radius && dy >= -radius && dy <= radius) &&
                            !((dz == -radius || dz == radius) && (dy == -radius || dy == radius));
                }
                break;
            case EAST:
                if (dx == radius) {
                    return (dz >= -radius && dz <= radius && dy >= -radius && dy <= radius) &&
                            !((dz == -radius || dz == radius) && (dy == -radius || dy == radius));
                }
                break;
            case UP:
                if (dy == radius) {
                    return (dx >= -radius && dx <= radius && dz >= -radius && dz <= radius) &&
                            !((dx == -radius || dx == radius) && (dz == -radius || dz == radius));
                }
                break;
            case DOWN:
                if (dy == -radius) {
                    return (dx >= -radius && dx <= radius && dz >= -radius && dz <= radius) &&
                            !((dx == -radius || dx == radius) && (dz == -radius || dz == radius));
                }
                break;
        }
        return false;
    }

    // 計算是否為直接接觸方塊
    private boolean calculateDirectContact(int dx, int dy, int dz) {
        int manhattanDistance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);

        // 檢查是否在玩家佔據的兩個方塊內
        if ((dx == 0 && dz == 0 && (dy == 0 || dy == 1))) {
            return false; // 玩家自身佔據的方塊
        }

        // 直接接觸：曼哈頓距離為1
        if (manhattanDistance == 1) {
            return true;
        }

        // 特殊：正下方方塊
        if (dx == 0 && dy == -1 && dz == 0) {
            return true;
        }

        return false;
    }

//    @Inject(method = "tick", at = @At("TAIL"))
//    private void onServerTick(CallbackInfo ci) {
//        tickCounter++;
//        if (tickCounter % 10 != 0) return;
//
//        for (ServerWorld world : ((MinecraftServer)(Object)this).getWorlds()) {
//            for (ServerPlayerEntity player : world.getPlayers()) {
//                if (player.getGameMode() == GameMode.SPECTATOR) continue;
//
//                BlockPos center = player.getBlockPos();
//                int totalRadius = 3;
//
//                // 獲取玩家準心看向的方向（更精確）
//                Vec3d lookVec = player.getRotationVec(1.0F);
//
//                // 標準化為主要方向（取最接近的軸向）
//                Direction facingDirection = Direction.getFacing(
//                        (float)lookVec.x,
//                        (float)lookVec.y,
//                        (float)lookVec.z
//                );
//
//                // 或者從Pitch和Yaw計算方向
//                float pitch = player.getPitch();
//                float yaw = player.getYaw();
//
//                // 簡化：根據俯仰角判斷上下，根據偏航角判斷水平方向
//                Direction primaryDirection;
//                if (pitch > 45) {
//                    primaryDirection = Direction.DOWN;
//                } else if (pitch < -45) {
//                    primaryDirection = Direction.UP;
//                } else {
//                    // 水平方向
//                    primaryDirection = Direction.getFacing(lookVec);
//                }
//
//                // 計算朝向方向上的額外方塊位置（在邊界外一格）
//                BlockPos extraBlockPos = center.offset(primaryDirection, totalRadius + 1);
//
//                // 處理整個範圍
//                for (int dx = -totalRadius; dx <= totalRadius; dx++) {
//                    for (int dy = -totalRadius; dy <= totalRadius; dy++) {
//                        for (int dz = -totalRadius; dz <= totalRadius; dz++) {
//                            BlockPos pos = center.add(dx, dy, dz);
//
//                            // 檢查是否為朝向的額外方塊
//                            boolean isExtraFacingBlock = pos.equals(extraBlockPos);
//
//                            // 也檢查朝向方向邊界上的方塊
//                            boolean isOnFacingEdge = false;
//                            switch (primaryDirection) {
//                                case NORTH:
//                                    isOnFacingEdge = (dz == -totalRadius);
//                                    break;
//                                case SOUTH:
//                                    isOnFacingEdge = (dz == totalRadius);
//                                    break;
//                                case WEST:
//                                    isOnFacingEdge = (dx == -totalRadius);
//                                    break;
//                                case EAST:
//                                    isOnFacingEdge = (dx == totalRadius);
//                                    break;
//                                case UP:
//                                    isOnFacingEdge = (dy == totalRadius);
//                                    break;
//                                case DOWN:
//                                    isOnFacingEdge = (dy == -totalRadius);
//                                    break;
//                            }
//
//                            // 計算是否為直接接觸方塊
//                            boolean isDirectContact = calculateDirectContact(dx, dy, dz);
//
//                            if (isDirectContact || isExtraFacingBlock) {
//                                // 發送真實數據
//                                BlockState realState = player.getWorld().getBlockState(pos);
//                                player.networkHandler.sendPacket(
//                                        new BlockUpdateS2CPacket(pos, realState)
//                                );
//                            } else if (isOnFacingEdge) {
//                                // 朝向邊界上的方塊也可以考慮顯示為真實（可選）
//                                BlockState realState = player.getWorld().getBlockState(pos);
//                                player.networkHandler.sendPacket(
//                                        new BlockUpdateS2CPacket(pos, realState)
//                                );
//                            } else {
//                                // 其他所有方塊發送空氣
//                                if (shouldShowAsAir(player.getServerWorld(), pos)) {
//                                    player.networkHandler.sendPacket(
//                                            new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState())
//                                    );
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private boolean calculateDirectContact(int dx, int dy, int dz) {
//        int manhattanDistance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
//
//        // 檢查是否在玩家佔據的兩個方塊內
//        if ((dx == 0 && dz == 0 && (dy == 0 || dy == 1))) {
//            return false; // 玩家自身佔據的方塊
//        }
//
//        // 直接接觸：曼哈頓距離為1
//        if (manhattanDistance == 1) {
//            return true;
//        }
//
//        // 特殊：正下方方塊
//        if (dx == 0 && dy == -1 && dz == 0) {
//            return true;
//        }
//
//        return false;
//    }

    private boolean shouldShowAsAir(ServerWorld world, BlockPos pos) {
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
                !currentState.isOf(Blocks.DEEPSLATE_REDSTONE_ORE)) {
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

            // 如果相鄰方塊是空氣或任何會被視為「空氣」的方塊
//            if (neighborBlock == Blocks.AIR ||
//                    neighborBlock == Blocks.CAVE_AIR ||
//                    neighborBlock == Blocks.VOID_AIR ||
//                    neighborBlock == Blocks.WATER ||
//                    neighborBlock == Blocks.LAVA ||
//                    neighborBlock == Blocks.GRASS_BLOCK ||
//                    neighborBlock == Blocks.TALL_GRASS ||
//                    neighborBlock == Blocks.FERN ||
//                    neighborBlock == Blocks.LARGE_FERN ||
//                    neighborBlock == Blocks.DEAD_BUSH ||
//                    neighborBlock == Blocks.SNOW ||
//                    neighborBlock == Blocks.VINE ||
//                    neighborBlock == Blocks.GLOW_LICHEN ||
//                    neighborBlock == Blocks.SEAGRASS ||
//                    neighborBlock == Blocks.TALL_SEAGRASS ||
//                    neighborBlock == Blocks.KELP ||
//                    neighborBlock == Blocks.KELP_PLANT) {
//                return true;
//            }
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

//    private boolean isAirOrRelated(Block block) {
//        return block == Blocks.AIR ||
//                block == Blocks.CAVE_AIR ||
//                block == Blocks.VOID_AIR ||
//                block == Blocks.WATER ||
//                block == Blocks.LAVA ||
//                block == Blocks.GRASS_BLOCK ||
//                block == Blocks.TALL_GRASS ||
//                block == Blocks.FERN ||
//                block == Blocks.LARGE_FERN ||
//                block == Blocks.DEAD_BUSH ||
//                block == Blocks.SNOW ||
//                block == Blocks.VINE ||
//                block == Blocks.GLOW_LICHEN ||
//                block == Blocks.SEAGRASS ||
//                block == Blocks.TALL_SEAGRASS ||
//                block == Blocks.KELP ||
//                block == Blocks.KELP_PLANT;
//    }

}

//@Mixin(MinecraftServer.class)
//public class AntiXrayMixin {
//    @Inject(method = "tick", at = @At("TAIL"))
//    private void onServerTick(CallbackInfo ci) {
//        for (ServerWorld world : ((MinecraftServer)(Object)this).getWorlds()) {
//            for (ServerPlayerEntity player : world.getPlayers()) {
//                BlockPos center = player.getBlockPos();
//
//                // 1. 處理玩家周圍3x3x3實心區域 - 發送真實方塊數據
//                for (int x = -2; x <= 2; x++) {
//                    for (int y = -2; y <= 2; y++) {
//                        for (int z = -2; z <= 2; z++) {
//                            BlockPos pos = center.add(x, y, z);
//                            // 發送真實方塊數據
//                            BlockState realState = player.getWorld().getBlockState(pos);
//                            player.networkHandler.sendPacket(
//                                    new BlockUpdateS2CPacket(pos, realState)
//                            );
//                        }
//                    }
//                }
//
//                // 2. 處理玩家周圍5x5x5空心區域 - 發送假空氣
//                int radius = 3; // 5x5x5 的半徑是2
//
//                // 生成空心立方體的外殼
//                for (int dx = -radius; dx <= radius; dx++) {
//                    for (int dy = -radius; dy <= radius; dy++) {
//                        for (int dz = -radius; dz <= radius; dz++) {
//                            // 檢查是否在外殼上（空心）
//                            boolean isOuterShell =
//                                    Math.abs(dx) == radius ||
//                                            Math.abs(dy) == radius ||
//                                            Math.abs(dz) == radius;
//
//                            // 並且不在3x3x3實心區域內
//                            boolean notInInnerCube =
//                                    Math.abs(dx) > 1 ||
//                                            Math.abs(dy) > 1 ||
//                                            Math.abs(dz) > 1;
//
//                            if (isOuterShell && notInInnerCube) {
//                                BlockPos pos = center.add(dx, dy, dz);
//
//                                // 只對特定方塊顯示為空氣
//                                BlockState currentState = player.getWorld().getBlockState(pos);
//                                if (currentState.isOf(STONE) ||
//                                        currentState.isOf(DEEPSLATE) ||
//                                        currentState.isOf(SUPER_DEEPSLATE_BLOCK)) {
//
//                                    player.networkHandler.sendPacket(
//                                            new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState())
//                                    );
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}