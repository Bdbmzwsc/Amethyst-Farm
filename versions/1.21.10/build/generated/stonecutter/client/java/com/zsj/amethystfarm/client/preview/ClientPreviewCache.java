package com.zsj.amethystfarm.client.preview;

import com.zsj.amethystfarm.client.render.CachedCrystalOutline;
import com.zsj.amethystfarm.farm.AmethystCrystalHelper;
import com.zsj.amethystfarm.network.PreviewSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPreviewCache {
    public record CrystalPreview(
        BlockPos origin,
        BlockPos extent,
        Map<BlockPos, CachedCrystalOutline> crystals,
        long updatedAt
    ) {
        public BlockPos pos1() {
            return origin;
        }

        public BlockPos pos2() {
            return origin.offset(extent.getX(), extent.getY(), extent.getZ());
        }

        public List<CachedCrystalOutline> crystalList() {
            return List.copyOf(crystals.values());
        }
    }

    private static final Map<Integer, CrystalPreview> PREVIEWS = new ConcurrentHashMap<>();

    private ClientPreviewCache() {}

    public static void apply(PreviewSyncPayload payload) {
        if (payload.action() == PreviewSyncPayload.CLEAR) {
            PREVIEWS.remove(payload.fakeEntityId());
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        switch (payload.action()) {
            case PreviewSyncPayload.FULL -> applyFull(client, payload);
            case PreviewSyncPayload.DELTA -> applyDelta(client, payload);
        }
    }

    private static void applyFull(Minecraft client, PreviewSyncPayload payload) {
        Map<BlockPos, CachedCrystalOutline> crystals = new HashMap<>();
        for (PreviewSyncPayload.CrystalEntry entry : payload.added()) {
            BlockPos pos = payload.origin().offset(entry.dx(), entry.dy(), entry.dz());
            CachedCrystalOutline outline = buildOutline(client, pos, entry.mature());
            if (outline != null) {
                crystals.put(pos, outline);
            }
        }

        PREVIEWS.put(payload.fakeEntityId(), new CrystalPreview(
            payload.origin(),
            payload.extent(),
            crystals,
            System.currentTimeMillis()
        ));
    }

    private static void applyDelta(Minecraft client, PreviewSyncPayload payload) {
        CrystalPreview existing = PREVIEWS.get(payload.fakeEntityId());
        Map<BlockPos, CachedCrystalOutline> crystals = existing != null
            ? new HashMap<>(existing.crystals())
            : new HashMap<>();

        BlockPos origin = payload.origin();
        for (BlockPos relative : payload.removedRelative()) {
            crystals.remove(origin.offset(relative.getX(), relative.getY(), relative.getZ()));
        }
        for (PreviewSyncPayload.CrystalEntry entry : payload.added()) {
            BlockPos pos = origin.offset(entry.dx(), entry.dy(), entry.dz());
            CachedCrystalOutline outline = buildOutline(client, pos, entry.mature());
            if (outline != null) {
                crystals.put(pos, outline);
            } else {
                crystals.remove(pos);
            }
        }

        BlockPos extent = existing != null ? existing.extent() : BlockPos.ZERO;
        PREVIEWS.put(payload.fakeEntityId(), new CrystalPreview(origin, extent, crystals, System.currentTimeMillis()));
    }

    private static CachedCrystalOutline buildOutline(Minecraft client, BlockPos pos, boolean matureHint) {
        BlockState state = client.level.getBlockState(pos);
        if (!AmethystCrystalHelper.isHarvestableCrystal(state)) {
            return null;
        }
        VoxelShape shape = state.getShape(client.level, pos);
        if (shape.isEmpty()) {
            return null;
        }
        boolean mature = matureHint || AmethystCrystalHelper.isMatureCluster(state);
        return new CachedCrystalOutline(pos, shape.bounds(), mature);
    }

    public static Map<Integer, CrystalPreview> all() {
        purgeMissingEntities();
        return PREVIEWS;
    }

    public static boolean isEmpty() {
        return PREVIEWS.isEmpty();
    }

    private static void purgeMissingEntities() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        Iterator<Map.Entry<Integer, CrystalPreview>> iterator = PREVIEWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, CrystalPreview> entry = iterator.next();
            if (client.level.getEntity(entry.getKey()) == null) {
                iterator.remove();
            }
        }
    }
}
