package com.zsj.amethystfarm.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class AmethystCrystalHelper {
    public static final List<Block> HARVESTABLE_BLOCKS = List.of(
        Blocks.SMALL_AMETHYST_BUD,
        Blocks.MEDIUM_AMETHYST_BUD,
        Blocks.LARGE_AMETHYST_BUD,
        Blocks.AMETHYST_CLUSTER
    );

    private AmethystCrystalHelper() {}

    public static boolean isHarvestableCrystal(BlockState state) {
        return HARVESTABLE_BLOCKS.stream().anyMatch(state::is);
    }

    public static boolean isMatureCluster(BlockState state) {
        return state.is(Blocks.AMETHYST_CLUSTER);
    }

    public static boolean isAttachedToBuddingAmethyst(BlockState state, Level level, BlockPos pos) {
        if (!isHarvestableCrystal(state)) {
            return false;
        }
        var facing = state.getValue(AmethystClusterBlock.FACING);
        return level.getBlockState(pos.relative(facing.getOpposite())).is(Blocks.BUDDING_AMETHYST);
    }

    public static boolean isOnBuddingSide(BlockState state) {
        if (!state.hasProperty(AmethystClusterBlock.FACING)) {
            return false;
        }
        Direction facing = state.getValue(AmethystClusterBlock.FACING);
        return facing.getAxis() != Direction.Axis.Y;
    }

    public static boolean isHarvestableCrystal(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        return isAttachedToBuddingAmethyst(state, level, pos);
    }

    public static boolean matchesHarvestWhitelist(
        BlockState state,
        Level level,
        BlockPos pos,
        HarvestWhitelist whitelist
    ) {
        if (!isAttachedToBuddingAmethyst(state, level, pos)) {
            return false;
        }
        if (whitelist.isSidesOnly() && !isOnBuddingSide(state)) {
            return false;
        }
        if (state.is(Blocks.SMALL_AMETHYST_BUD)) {
            return whitelist.isSmallBud();
        }
        if (state.is(Blocks.MEDIUM_AMETHYST_BUD)) {
            return whitelist.isMediumBud();
        }
        if (state.is(Blocks.LARGE_AMETHYST_BUD)) {
            return whitelist.isLargeBud();
        }
        if (state.is(Blocks.AMETHYST_CLUSTER)) {
            return whitelist.isCluster();
        }
        return false;
    }

    public static boolean matchesHarvestWhitelist(
        BlockState state,
        net.minecraft.server.level.ServerLevel level,
        BlockPos pos,
        HarvestWhitelist whitelist
    ) {
        return matchesHarvestWhitelist(state, (Level) level, pos, whitelist);
    }

    public static int getGrowthPriority(BlockState state) {
        if (state.is(Blocks.AMETHYST_CLUSTER)) {
            return 4;
        }
        if (state.is(Blocks.LARGE_AMETHYST_BUD)) {
            return 3;
        }
        if (state.is(Blocks.MEDIUM_AMETHYST_BUD)) {
            return 2;
        }
        if (state.is(Blocks.SMALL_AMETHYST_BUD)) {
            return 1;
        }
        return 0;
    }

    public static java.util.Comparator<BlockPos> harvestPriorityComparator(net.minecraft.server.level.ServerLevel level) {
        return java.util.Comparator
            .comparingInt((BlockPos pos) -> -getGrowthPriority(level.getBlockState(pos)))
            .thenComparingDouble(pos -> 0);
    }
}
