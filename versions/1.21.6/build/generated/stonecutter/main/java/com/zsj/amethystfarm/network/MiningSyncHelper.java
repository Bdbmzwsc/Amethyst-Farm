package com.zsj.amethystfarm.network;

import com.zsj.amethystfarm.config.AmethystFarmSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ConcurrentHashMap;

public final class MiningSyncHelper {
    private static final ConcurrentHashMap<Integer, BlockPos> LAST_TARGETS = new ConcurrentHashMap<>();

    private MiningSyncHelper() {}

    public static void broadcastTarget(ServerPlayer fake, BlockPos target) {
        if (!(fake.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos previous = LAST_TARGETS.get(fake.getId());
        if (target.equals(previous)) {
            return;
        }
        LAST_TARGETS.put(fake.getId(), target.immutable());

        NetworkBroadcast.sendToWorldNear(
            level,
            target,
            AmethystFarmSettings.maxRenderDistance + 8,
            MiningTargetPayload.of(fake.getId(), target),
            MiningTargetPayload.TYPE
        );
    }

    public static void broadcastClear(ServerPlayer fake) {
        if (!(fake.level() instanceof ServerLevel level)) {
            return;
        }

        if (LAST_TARGETS.remove(fake.getId()) == null) {
            return;
        }

        NetworkBroadcast.sendToWorld(level, MiningTargetPayload.cleared(fake.getId()), MiningTargetPayload.TYPE);
    }
}
