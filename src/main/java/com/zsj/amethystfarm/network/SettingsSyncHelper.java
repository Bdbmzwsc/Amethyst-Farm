package com.zsj.amethystfarm.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class SettingsSyncHelper {
    private SettingsSyncHelper() {}

    public static void sendToPlayer(ServerPlayer player) {
        NetworkBroadcast.sendToPlayer(player, SettingsSyncPayload.fromSettings(), SettingsSyncPayload.TYPE);
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(player);
        }
    }
}
