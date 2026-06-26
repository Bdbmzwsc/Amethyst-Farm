package com.zsj.amethystfarm.client.preview;

import com.zsj.amethystfarm.client.render.CachedCrystalOutline;
import com.zsj.amethystfarm.farm.AmethystCrystalHelper;
import com.zsj.amethystfarm.network.MiningTargetPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientMiningCache {
    private static final Map<Integer, CachedCrystalOutline> MINING_TARGETS = new ConcurrentHashMap<>();

    private ClientMiningCache() {}

    public static void apply(MiningTargetPayload payload) {
        if (payload.action() == MiningTargetPayload.CLEAR) {
            MINING_TARGETS.remove(payload.fakeEntityId());
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        BlockPos pos = payload.target();
        BlockState state = client.level.getBlockState(pos);
        if (!AmethystCrystalHelper.isHarvestableCrystal(state)) {
            MINING_TARGETS.remove(payload.fakeEntityId());
            return;
        }

        VoxelShape shape = state.getShape(client.level, pos);
        if (shape.isEmpty()) {
            MINING_TARGETS.remove(payload.fakeEntityId());
            return;
        }

        MINING_TARGETS.put(payload.fakeEntityId(), new CachedCrystalOutline(
            pos, shape.bounds(), AmethystCrystalHelper.isMatureCluster(state)
        ));
    }

    public static Map<Integer, CachedCrystalOutline> all() {
        purgeMissingEntities();
        return MINING_TARGETS;
    }

    public static boolean isEmpty() {
        return MINING_TARGETS.isEmpty();
    }

    private static void purgeMissingEntities() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        Iterator<Map.Entry<Integer, CachedCrystalOutline>> iterator = MINING_TARGETS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, CachedCrystalOutline> entry = iterator.next();
            if (client.level.getEntity(entry.getKey()) == null) {
                iterator.remove();
            }
        }
    }
}
