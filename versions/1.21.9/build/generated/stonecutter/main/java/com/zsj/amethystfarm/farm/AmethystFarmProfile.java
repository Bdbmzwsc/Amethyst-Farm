package com.zsj.amethystfarm.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import com.zsj.amethystfarm.util.NbtCompat;

public class AmethystFarmProfile {
    private WorkMode workMode = WorkMode.IDLE;
    /** When false this fake is ignored entirely and Carpet ActionPack is not touched. */
    private boolean botEnabled = false;
    private final HarvestWhitelist harvestWhitelist = new HarvestWhitelist();
    private BlockPos lockedTarget = null;
    private int tickCounter;
    private int buddingCount;
    private int crystalCount;
    private int clusterCount;
    private int reachableCrystalCount;
    private boolean scanPreviewActive;
    private BlockPos miningTarget;
    private boolean miningAttackActive;
    private boolean tickPhaseInitialized;
    private BlockPos breakingBlockPos;
    private float blockBreakDamage;
    private int blockHitDelay;

    public WorkMode getWorkMode() {
        return workMode;
    }

    public void setWorkMode(WorkMode workMode) {
        this.workMode = workMode;
        if (workMode != WorkMode.LOCK_MINE) {
            lockedTarget = null;
        }
    }

    public boolean isBotEnabled() {
        return botEnabled;
    }

    public void setBotEnabled(boolean botEnabled) {
        this.botEnabled = botEnabled;
    }

    public HarvestWhitelist getHarvestWhitelist() {
        return harvestWhitelist;
    }

    public BlockPos getLockedTarget() {
        return lockedTarget;
    }

    public void setLockedTarget(BlockPos lockedTarget) {
        this.lockedTarget = lockedTarget == null ? null : lockedTarget.immutable();
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public void setTickCounter(int tickCounter) {
        this.tickCounter = tickCounter;
    }

    public int getBuddingCount() {
        return buddingCount;
    }

    public void setBuddingCount(int buddingCount) {
        this.buddingCount = buddingCount;
    }

    public int getCrystalCount() {
        return crystalCount;
    }

    public void setCrystalCount(int crystalCount) {
        this.crystalCount = crystalCount;
    }

    public int getClusterCount() {
        return clusterCount;
    }

    public void setClusterCount(int clusterCount) {
        this.clusterCount = clusterCount;
    }

    public int getReachableCrystalCount() {
        return reachableCrystalCount;
    }

    public void setReachableCrystalCount(int reachableCrystalCount) {
        this.reachableCrystalCount = reachableCrystalCount;
    }

    public void ensureTickPhase(java.util.UUID botId, int interval) {
        if (!tickPhaseInitialized) {
            tickCounter = Math.floorMod(botId.hashCode(), Math.max(1, interval));
            tickPhaseInitialized = true;
        }
    }

    public void markScanPreviewActive() {
        scanPreviewActive = true;
    }

    public boolean clearScanPreviewIfNeeded() {
        if (scanPreviewActive) {
            scanPreviewActive = false;
            return true;
        }
        return false;
    }

    public BlockPos getMiningTarget() {
        return miningTarget;
    }

    public void setMiningTarget(BlockPos target) {
        this.miningTarget = target == null ? null : target.immutable();
    }

    public boolean clearMiningTargetIfNeeded() {
        if (miningTarget != null) {
            miningTarget = null;
            return true;
        }
        return false;
    }

    public boolean isMiningAttackActive() {
        return miningAttackActive;
    }

    public void setMiningAttackActive(boolean active) {
        this.miningAttackActive = active;
    }

    public void clearMiningAttack() {
        miningAttackActive = false;
    }

    public BlockPos getBreakingBlockPos() {
        return breakingBlockPos;
    }

    public void setBreakingBlockPos(BlockPos pos) {
        this.breakingBlockPos = pos == null ? null : pos.immutable();
    }

    public float getBlockBreakDamage() {
        return blockBreakDamage;
    }

    public void setBlockBreakDamage(float damage) {
        this.blockBreakDamage = damage;
    }

    public void addBlockBreakDamage(float delta) {
        this.blockBreakDamage += delta;
    }

    public int getBlockHitDelay() {
        return blockHitDelay;
    }

    public void setBlockHitDelay(int delay) {
        this.blockHitDelay = delay;
    }

    public void clearBreakingState() {
        breakingBlockPos = null;
        blockBreakDamage = 0.0f;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("mode", workMode.name());
        tag.putBoolean("botEnabled", botEnabled);
        tag.put("harvestWhitelist", harvestWhitelist.save());
        if (lockedTarget != null) {
            tag.putLong("lockedTarget", lockedTarget.asLong());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        NbtCompat.readString(tag, "mode", value -> workMode = WorkMode.valueOf(value));
        if (tag.contains("botEnabled")) {
            botEnabled = NbtCompat.readBoolean(tag, "botEnabled", false);
        } else {
            botEnabled = workMode != WorkMode.IDLE;
        }
        NbtCompat.readCompound(tag, "harvestWhitelist", harvestWhitelist::load);
        NbtCompat.readLong(tag, "lockedTarget", value -> lockedTarget = BlockPos.of(value));
    }
}
