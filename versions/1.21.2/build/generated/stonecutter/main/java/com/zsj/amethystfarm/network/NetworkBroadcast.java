package com.zsj.amethystfarm.network;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class NetworkBroadcast {
    private NetworkBroadcast() {}

    public static void sendToWorld(ServerLevel level, CustomPacketPayload payload, CustomPacketPayload.Type<?> type) {
        for (ServerPlayer player : PlayerLookup.world(level)) {
            sendToPlayer(player, payload, type);
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload.Type<?> type) {
        if (ServerPlayNetworking.canSend(player, type)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendToWorldNear(
        ServerLevel level,
        BlockPos center,
        double range,
        CustomPacketPayload payload,
        CustomPacketPayload.Type<?> type
    ) {
        double rangeSqr = range * range;
        Vec3 point = Vec3.atCenterOf(center);
        for (ServerPlayer player : PlayerLookup.world(level)) {
            if (player.level().dimension() != level.dimension()) {
                continue;
            }
            if (player.getEyePosition().distanceToSqr(point) > rangeSqr) {
                continue;
            }
            sendToPlayer(player, payload, type);
        }
    }
}
