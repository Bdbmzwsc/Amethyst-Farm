package com.zsj.amethystfarm.farm;

import com.zsj.amethystfarm.config.AmethystFarmSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class AmethystScanner {
    private AmethystScanner() {}

    public record ScanResult(List<BlockPos> buddingBlocks, List<BlockPos> harvestableCrystals) {
        public int matureClusterCount(ServerLevel level) {
            return (int) harvestableCrystals.stream()
                .filter(pos -> AmethystCrystalHelper.isMatureCluster(level.getBlockState(pos)))
                .count();
        }

        public int inReachCount(ServerPlayer player) {
            return AmethystReachHelper.filterInReach(player, harvestableCrystals).size();
        }
    }

    public static ScanResult scan(ServerLevel level, AmethystFarmBinding binding, HarvestWhitelist whitelist) {
        List<BlockPos> budding = new ArrayList<>();
        List<BlockPos> crystals = new ArrayList<>();

        if (!binding.isBound()) {
            return new ScanResult(budding, crystals);
        }

        BlockPos min = binding.getMin();
        BlockPos max = binding.getMax();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.BUDDING_AMETHYST)) {
                        budding.add(pos.immutable());
                    } else if (AmethystCrystalHelper.matchesHarvestWhitelist(state, level, pos, whitelist)) {
                        crystals.add(pos.immutable());
                    }
                }
            }
        }

        return new ScanResult(budding, crystals);
    }

    public static ScanResult scan(ServerLevel level, AmethystFarmBinding binding) {
        HarvestWhitelist all = new HarvestWhitelist();
        all.setSidesOnly(false);
        return scan(level, binding, all);
    }

    public static ScanResult scanNearby(
        ServerLevel level,
        ServerPlayer player,
        HarvestWhitelist whitelist,
        int radius
    ) {
        return scan(level, AmethystFarmBinding.around(player.blockPosition(), radius, level.dimension()), whitelist);
    }

    public static AmethystFarmBinding nearbyBounds(ServerPlayer player, int radius) {
        return AmethystFarmBinding.around(player.blockPosition(), radius, player.level().dimension());
    }

    public static BlockPos findNearestCrystal(ServerPlayer player, HarvestWhitelist whitelist, int radius) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        ScanResult result = scanNearby(level, player, whitelist, radius);
        return selectHarvestTarget(player, level, result.harvestableCrystals());
    }

    public static BlockPos findCrystalInLookDirection(ServerPlayer player, HarvestWhitelist whitelist) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }

        double reach = AmethystReachHelper.getMiningReach(player);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (double d = 0; d <= reach; d += 0.25) {
            Vec3 sample = eye.add(look.scale(d));
            mutable.set(sample.x, sample.y, sample.z);
            BlockState state = level.getBlockState(mutable);
            if (AmethystCrystalHelper.matchesHarvestWhitelist(state, level, mutable, whitelist)
                && MiningClaimRegistry.isAvailable(level.dimension(), player.getUUID(), mutable)) {
                return mutable.immutable();
            }
        }

        return null;
    }

    public static BlockPos selectHarvestTarget(ServerPlayer player, ServerLevel level, List<BlockPos> crystals) {
        List<BlockPos> inReach = AmethystReachHelper.filterInReach(player, crystals);
        List<BlockPos> available = MiningClaimRegistry.filterAvailable(player, inReach);
        return available.stream()
            .min(AmethystCrystalHelper.harvestPriorityComparator(level)
                .thenComparingDouble(pos -> player.distanceToSqr(Vec3.atCenterOf(pos))))
            .orElse(null);
    }

    public static void spawnPreviewParticles(ServerLevel level, AmethystFarmBinding binding, ScanResult result) {
        if (!AmethystFarmSettings.scanPreview) {
            return;
        }

        BlockPos min = binding.getMin();
        BlockPos max = binding.getMax();
        spawnCornerParticles(level, min);
        spawnCornerParticles(level, max);
        spawnCornerParticles(level, new BlockPos(min.getX(), min.getY(), max.getZ()));
        spawnCornerParticles(level, new BlockPos(min.getX(), max.getY(), min.getZ()));
        spawnCornerParticles(level, new BlockPos(max.getX(), min.getY(), min.getZ()));
        spawnCornerParticles(level, new BlockPos(max.getX(), max.getY(), min.getZ()));
        spawnCornerParticles(level, new BlockPos(min.getX(), max.getY(), max.getZ()));
        spawnCornerParticles(level, new BlockPos(max.getX(), min.getY(), max.getZ()));

        for (BlockPos pos : result.buddingBlocks()) {
            level.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                1, 0.05, 0.05, 0.05, 0.0
            );
        }

        for (BlockPos pos : result.harvestableCrystals()) {
            BlockState state = level.getBlockState(pos);
            if (AmethystCrystalHelper.isMatureCluster(state)) {
                level.sendParticles(
                    ParticleTypes.WAX_ON,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.1, 0.1, 0.1, 0.01
                );
            } else {
                level.sendParticles(
                    ParticleTypes.WITCH,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    2, 0.08, 0.08, 0.08, 0.01
                );
            }
        }
    }

    private static void spawnCornerParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(
            ParticleTypes.GLOW,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            2, 0.0, 0.0, 0.0, 0.0
        );
    }
}
