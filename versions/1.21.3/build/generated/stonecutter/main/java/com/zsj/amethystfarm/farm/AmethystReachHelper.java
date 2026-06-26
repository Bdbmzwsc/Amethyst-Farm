package com.zsj.amethystfarm.farm;

import com.zsj.amethystfarm.config.AmethystFarmSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class AmethystReachHelper {
    private AmethystReachHelper() {}

    public static double getMiningReach(ServerPlayer player) {
        if (AmethystFarmSettings.miningReach > 0) {
            return AmethystFarmSettings.miningReach;
        }
        return player.gameMode.getGameModeForPlayer().isCreative() ? 5.0D : 4.5D;
    }

    public static double getReachSquared(ServerPlayer player) {
        double reach = getMiningReach(player);
        return reach * reach;
    }

    public static boolean isWithinReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= getReachSquared(player);
    }

    public static List<BlockPos> filterInReach(ServerPlayer player, List<BlockPos> crystals) {
        double reachSqr = getReachSquared(player);
        return crystals.stream()
            .filter(pos -> player.distanceToSqr(Vec3.atCenterOf(pos)) <= reachSqr)
            .toList();
    }
}
