package com.zsj.amethystfarm.farm;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import com.zsj.amethystfarm.config.AmethystFarmSettings;
import com.zsj.amethystfarm.fakes.AmethystFarmBotAccess;
import com.zsj.amethystfarm.network.MiningSyncHelper;
import com.zsj.amethystfarm.network.PreviewSyncHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AmethystHarvester {
    private AmethystHarvester() {}

    public static void enableBot(AmethystFarmProfile profile) {
        profile.setBotEnabled(true);
    }

    /** Disables per-bot control and releases any in-progress farm actions once. */
    public static void disableBot(ServerPlayer fake, AmethystFarmProfile profile) {
        profile.setBotEnabled(false);
        profile.setWorkMode(WorkMode.IDLE);
        profile.setLockedTarget(null);
        if (!(fake.level() instanceof ServerLevel level)) {
            return;
        }
        if (profile.clearScanPreviewIfNeeded()) {
            PreviewSyncHelper.broadcastClear(fake);
        }
        releaseMining(fake, level, profile, true);
    }

    public static void applyWorkMode(ServerPlayer fake, AmethystFarmProfile profile, WorkMode mode) {
        WorkMode previous = profile.getWorkMode();
        if (mode != WorkMode.IDLE) {
            profile.setBotEnabled(true);
        }
        profile.setWorkMode(mode);
        if (!(fake.level() instanceof ServerLevel level)) {
            return;
        }
        if (isMiningMode(previous) && !isMiningMode(mode)) {
            releaseMining(fake, level, profile, true);
        }
        if (previous == WorkMode.SCAN && mode != WorkMode.SCAN && profile.clearScanPreviewIfNeeded()) {
            PreviewSyncHelper.broadcastClear(fake);
        }
    }

    public static void tickFakePlayer(ServerPlayer fake, AmethystFarmProfile profile) {
        if (!AmethystFarmSettings.enabled || !profile.isBotEnabled()) {
            return;
        }

        if (!(fake.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isMiningMode(profile.getWorkMode()) && hasActiveMiningState(profile)) {
            releaseMining(fake, level, profile, true);
        }

        profile.ensureTickPhase(fake.getUUID(), AmethystFarmSettings.scanInterval);

        if (profile.getWorkMode() != WorkMode.SCAN && profile.clearScanPreviewIfNeeded()) {
            PreviewSyncHelper.broadcastClear(fake);
        }

        profile.setTickCounter(profile.getTickCounter() + 1);
        if (profile.getTickCounter() < AmethystFarmSettings.scanInterval) {
            if (isMiningMode(profile.getWorkMode())) {
                continueMiningTarget(fake, level, profile);
            }
            return;
        }
        profile.setTickCounter(0);

        if (nearbyScanVolume() > AmethystFarmSettings.maxScanVolume) {
            return;
        }

        switch (profile.getWorkMode()) {
            case SCAN -> handleScan(fake, level, profile);
            case HARVEST -> handleHarvest(fake, level, profile);
            case LOCK_MINE -> handleLockMine(fake, level, profile);
            case IDLE -> {
            }
        }
    }

    private static boolean isMiningMode(WorkMode mode) {
        return mode == WorkMode.HARVEST || mode == WorkMode.LOCK_MINE;
    }

    private static boolean hasActiveMiningState(AmethystFarmProfile profile) {
        return profile.getMiningTarget() != null
            || profile.getBreakingBlockPos() != null
            || profile.isMiningAttackActive();
    }

    private static int nearbyScanVolume() {
        int radius = AmethystFarmSettings.scanRadius;
        int edge = radius * 2 + 1;
        return edge * edge * edge;
    }

    private static int scanRadiusFor(ServerPlayer fake) {
        return AmethystFarmSettings.scanRadius;
    }

    private static boolean isValidMiningTarget(
        ServerPlayer fake,
        ServerLevel level,
        AmethystFarmProfile profile,
        BlockPos target
    ) {
        BlockState state = level.getBlockState(target);
        if (!AmethystCrystalHelper.matchesHarvestWhitelist(state, level, target, profile.getHarvestWhitelist())) {
            return false;
        }
        if (!AmethystReachHelper.isWithinReach(fake, target)) {
            return false;
        }
        return MiningClaimRegistry.isAvailable(level.dimension(), fake.getUUID(), target);
    }

    private static void releaseMining(
        ServerPlayer fake,
        ServerLevel level,
        AmethystFarmProfile profile,
        boolean stopActionPack
    ) {
        boolean hadActivity = hasActiveMiningState(profile);
        if (profile.clearMiningTargetIfNeeded()) {
            MiningSyncHelper.broadcastClear(fake);
        }
        profile.clearMiningAttack();
        AmethystBlockBreaker.abort(fake, level, profile);
        MiningClaimRegistry.release(fake);
        if (stopActionPack && hadActivity) {
            actionPack(fake).stopAll();
        }
    }

    private static void continueMiningTarget(ServerPlayer fake, ServerLevel level, AmethystFarmProfile profile) {
        BlockPos target = profile.getMiningTarget();
        if (target == null) {
            if (profile.getWorkMode() == WorkMode.HARVEST && AmethystFarmSettings.autoHarvest) {
                tryAssignHarvestTarget(fake, level, profile);
            }
            return;
        }
        if (!isValidMiningTarget(fake, level, profile, target)) {
            releaseMining(fake, level, profile, true);
            return;
        }
        maintainMining(fake, level, profile, target);
    }

    private static void handleScan(ServerPlayer fake, ServerLevel level, AmethystFarmProfile profile) {
        int radius = scanRadiusFor(fake);
        HarvestWhitelist whitelist = profile.getHarvestWhitelist();
        AmethystFarmBinding bounds = AmethystScanner.nearbyBounds(fake, radius);
        AmethystScanner.ScanResult result = AmethystScanner.scanNearby(level, fake, whitelist, radius);
        updateScanStats(profile, result, level, fake);
        AmethystScanner.spawnPreviewParticles(level, bounds, result);
        ((AmethystFarmBotAccess) fake).amethystfarm$setPreviewBinding(bounds, result);
        profile.markScanPreviewActive();
        PreviewSyncHelper.broadcastScan(fake, bounds, result);
    }

    private static void handleHarvest(ServerPlayer fake, ServerLevel level, AmethystFarmProfile profile) {
        if (!AmethystFarmSettings.autoHarvest) {
            return;
        }

        BlockPos currentTarget = profile.getMiningTarget();
        if (currentTarget != null && isValidMiningTarget(fake, level, profile, currentTarget)) {
            mineCrystal(fake, level, profile, currentTarget);
            return;
        }

        tryAssignHarvestTarget(fake, level, profile);
    }

    private static void tryAssignHarvestTarget(ServerPlayer fake, ServerLevel level, AmethystFarmProfile profile) {
        AmethystScanner.ScanResult result = AmethystScanner.scanNearby(
            level, fake, profile.getHarvestWhitelist(), scanRadiusFor(fake)
        );
        updateScanStats(profile, result, level, fake);

        List<BlockPos> candidates = rankedHarvestCandidates(fake, level, result.harvestableCrystals());
        if (candidates.isEmpty()) {
            releaseMining(fake, level, profile, hasActiveMiningState(profile));
            return;
        }

        for (BlockPos target : candidates) {
            if (beginMiningCrystal(fake, level, profile, target)) {
                maintainMining(fake, level, profile, target);
                return;
            }
        }

        releaseMining(fake, level, profile, hasActiveMiningState(profile));
    }

    private static List<BlockPos> rankedHarvestCandidates(
        ServerPlayer fake,
        ServerLevel level,
        List<BlockPos> crystals
    ) {
        List<BlockPos> inReach = AmethystReachHelper.filterInReach(fake, crystals);
        List<BlockPos> available = MiningClaimRegistry.filterAvailable(fake, inReach);
        Comparator<BlockPos> order = AmethystCrystalHelper.harvestPriorityComparator(level)
            .thenComparingDouble(pos -> fake.distanceToSqr(AmethystBlockBreaker.lookPoint(level.getBlockState(pos), pos)));
        List<BlockPos> ranked = new ArrayList<>(available);
        ranked.sort(order);
        return ranked;
    }

    private static void handleLockMine(ServerPlayer fake, ServerLevel level, AmethystFarmProfile profile) {
        BlockPos target = profile.getLockedTarget();

        if (target == null || !isValidMiningTarget(fake, level, profile, target)) {
            HarvestWhitelist whitelist = profile.getHarvestWhitelist();
            target = AmethystScanner.findCrystalInLookDirection(fake, whitelist);
            if (target == null) {
                List<BlockPos> candidates = rankedHarvestCandidates(
                    fake,
                    level,
                    AmethystScanner.scanNearby(level, fake, whitelist, scanRadiusFor(fake)).harvestableCrystals()
                );
                target = candidates.isEmpty() ? null : candidates.getFirst();
            }
            profile.setLockedTarget(target);
        }

        if (target == null) {
            releaseMining(fake, level, profile, hasActiveMiningState(profile));
            return;
        }

        mineCrystal(fake, level, profile, target);

        if (!AmethystCrystalHelper.matchesHarvestWhitelist(
            level.getBlockState(target), level, target, profile.getHarvestWhitelist())) {
            profile.setLockedTarget(null);
            releaseMining(fake, level, profile, true);
        }
    }

    private static void updateScanStats(
        AmethystFarmProfile profile,
        AmethystScanner.ScanResult result,
        ServerLevel level,
        ServerPlayer fake
    ) {
        profile.setBuddingCount(result.buddingBlocks().size());
        profile.setCrystalCount(result.harvestableCrystals().size());
        profile.setClusterCount(result.matureClusterCount(level));
        profile.setReachableCrystalCount(result.inReachCount(fake));
    }

    private static void mineCrystal(ServerPlayer fake, ServerLevel level, AmethystFarmProfile profile, BlockPos target) {
        if (!beginMiningCrystal(fake, level, profile, target)) {
            return;
        }
        maintainMining(fake, level, profile, target);
    }

    private static boolean beginMiningCrystal(
        ServerPlayer fake,
        ServerLevel level,
        AmethystFarmProfile profile,
        BlockPos target
    ) {
        if (!isValidMiningTarget(fake, level, profile, target)) {
            return false;
        }

        BlockPos previousTarget = profile.getMiningTarget();
        boolean sameTarget = target.equals(previousTarget);

        if (!sameTarget) {
            if (!MiningClaimRegistry.tryClaim(fake, target)) {
                return false;
            }
            BlockPos breaking = profile.getBreakingBlockPos();
            if (breaking != null && !breaking.equals(target)) {
                AmethystBlockBreaker.abort(fake, level, profile);
            }
            profile.setMiningTarget(target);
            MiningSyncHelper.broadcastTarget(fake, target);
            profile.clearMiningAttack();
        }

        return true;
    }

    private static void maintainMining(
        ServerPlayer fake,
        ServerLevel level,
        AmethystFarmProfile profile,
        BlockPos target
    ) {
        if (!isValidMiningTarget(fake, level, profile, target)) {
            releaseMining(fake, level, profile, true);
            return;
        }

        AmethystBlockBreaker.tickBreak(fake, level, profile, target, profile.getHarvestWhitelist());

        if (!AmethystCrystalHelper.matchesHarvestWhitelist(
            level.getBlockState(target), level, target, profile.getHarvestWhitelist())) {
            releaseMining(fake, level, profile, true);
        }
    }

    private static EntityPlayerActionPack actionPack(ServerPlayer fake) {
        return ((ServerPlayerInterface) fake).getActionPack();
    }
}
