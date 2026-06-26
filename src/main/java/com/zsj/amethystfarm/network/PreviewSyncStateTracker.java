package com.zsj.amethystfarm.network;

import com.zsj.amethystfarm.farm.AmethystCrystalHelper;
import com.zsj.amethystfarm.farm.AmethystFarmBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PreviewSyncStateTracker {
    private static final ConcurrentHashMap<Integer, PreviewState> STATES = new ConcurrentHashMap<>();

    private PreviewSyncStateTracker() {}

    public static void clear(int entityId) {
        STATES.remove(entityId);
    }

    public static Optional<PreviewSyncPayload> buildUpdate(
        int entityId,
        AmethystFarmBinding binding,
        List<BlockPos> crystals,
        ServerLevel level
    ) {
        BlockPos origin = binding.getMin();
        BlockPos max = binding.getMax();
        BlockPos extent = new BlockPos(
            max.getX() - origin.getX(),
            max.getY() - origin.getY(),
            max.getZ() - origin.getZ()
        );

        Set<BlockPos> current = new HashSet<>(crystals);
        PreviewState state = STATES.computeIfAbsent(entityId, ignored -> new PreviewState());

        boolean bindingChanged = !origin.equals(state.origin) || !extent.equals(state.extent);
        if (bindingChanged || state.crystals.isEmpty()) {
            state.origin = origin;
            state.extent = extent;
            state.crystals = current;
            return Optional.of(PreviewSyncPayload.full(entityId, origin, extent, toEntries(origin, current, level)));
        }

        if (state.crystals.equals(current)) {
            return Optional.empty();
        }

        Set<BlockPos> added = new HashSet<>(current);
        added.removeAll(state.crystals);
        Set<BlockPos> removed = new HashSet<>(state.crystals);
        removed.removeAll(current);
        state.crystals = current;

        if (added.size() + removed.size() > Math.max(1, current.size()) * 0.6) {
            return Optional.of(PreviewSyncPayload.full(entityId, origin, extent, toEntries(origin, current, level)));
        }

        return Optional.of(PreviewSyncPayload.delta(
            entityId,
            origin,
            toEntries(origin, added, level),
            toRelative(origin, removed)
        ));
    }

    private static List<PreviewSyncPayload.CrystalEntry> toEntries(BlockPos origin, Set<BlockPos> positions, ServerLevel level) {
        List<PreviewSyncPayload.CrystalEntry> entries = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            entries.add(new PreviewSyncPayload.CrystalEntry(
                pos.getX() - origin.getX(),
                pos.getY() - origin.getY(),
                pos.getZ() - origin.getZ(),
                AmethystCrystalHelper.isMatureCluster(state)
            ));
        }
        return entries;
    }

    private static List<BlockPos> toRelative(BlockPos origin, Set<BlockPos> positions) {
        List<BlockPos> relative = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            relative.add(new BlockPos(
                pos.getX() - origin.getX(),
                pos.getY() - origin.getY(),
                pos.getZ() - origin.getZ()
            ));
        }
        return relative;
    }

    private static final class PreviewState {
        private BlockPos origin = BlockPos.ZERO;
        private BlockPos extent = BlockPos.ZERO;
        private Set<BlockPos> crystals = Set.of();
    }
}
