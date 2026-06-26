package com.zsj.amethystfarm.network;

import com.zsj.amethystfarm.config.AmethystFarmSettings;
import com.zsj.amethystfarm.farm.AmethystFarmBinding;
import com.zsj.amethystfarm.farm.AmethystScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PreviewSyncHelper {
    private PreviewSyncHelper() {}

    public static void broadcastScan(ServerPlayer fake, AmethystFarmBinding binding, AmethystScanner.ScanResult result) {
        if (!(fake.level() instanceof ServerLevel level) || !AmethystFarmSettings.scanPreview) {
            return;
        }

        List<BlockPos> crystals = trimCrystals(result.harvestableCrystals(), fake.position(), AmethystFarmSettings.previewCrystalLimit);
        PreviewSyncStateTracker.buildUpdate(fake.getId(), binding, crystals, level).ifPresent(payload ->
            NetworkBroadcast.sendToWorldNear(
                level,
                binding.getMin(),
                AmethystFarmSettings.maxRenderDistance + 16,
                payload,
                PreviewSyncPayload.TYPE
            )
        );
    }

    public static void broadcastClear(ServerPlayer fake) {
        if (!(fake.level() instanceof ServerLevel level)) {
            return;
        }
        PreviewSyncStateTracker.clear(fake.getId());
        NetworkBroadcast.sendToWorld(level, PreviewSyncPayload.cleared(fake.getId()), PreviewSyncPayload.TYPE);
    }

    private static List<BlockPos> trimCrystals(List<BlockPos> crystals, Vec3 origin, int limit) {
        if (crystals.size() <= limit) {
            return crystals;
        }

        List<BlockPos> sorted = new ArrayList<>(crystals);
        sorted.sort(Comparator.comparingDouble(pos -> pos.getCenter().distanceToSqr(origin)));
        return List.copyOf(sorted.subList(0, limit));
    }
}
