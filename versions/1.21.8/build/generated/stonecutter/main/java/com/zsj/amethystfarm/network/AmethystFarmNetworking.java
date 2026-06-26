package com.zsj.amethystfarm.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class AmethystFarmNetworking {
    private AmethystFarmNetworking() {}

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playS2C().register(PreviewSyncPayload.TYPE, PreviewSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MiningTargetPayload.TYPE, MiningTargetPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SettingsSyncPayload.TYPE, SettingsSyncPayload.CODEC);
    }
}
