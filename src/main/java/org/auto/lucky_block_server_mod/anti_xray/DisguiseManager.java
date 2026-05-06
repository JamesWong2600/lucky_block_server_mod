package org.auto.lucky_block_server_mod.anti_xray;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DisguiseManager {
    public static final Map<ChunkPos, Set<BlockPos>> hiddenEmeralds = new HashMap<>();

    public static void addHiddenBlock(BlockPos pos) {
        hiddenEmeralds.computeIfAbsent(new ChunkPos(pos), k -> new HashSet<>()).add(pos);
    }

    public static boolean isHidden(BlockPos pos) {
        Set<BlockPos> set = hiddenEmeralds.get(new ChunkPos(pos));
        return set != null && set.contains(pos);
    }
}
