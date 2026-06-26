package com.zsj.amethystfarm;

import net.minecraft.server.level.ServerPlayer;

public final class CarpetHooks {
    private static final String FAKE_PLAYER_CLASS = "carpet.patches.EntityPlayerMPFake";

    private CarpetHooks() {}

    public static boolean isFakePlayer(ServerPlayer player) {
        return AmethystFarmRuntime.isCarpetLoaded()
            && FAKE_PLAYER_CLASS.equals(player.getClass().getName());
    }

    public static java.util.List<ServerPlayer> getOnlineFakePlayers(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
            .filter(CarpetHooks::isFakePlayer)
            .toList();
    }
}
