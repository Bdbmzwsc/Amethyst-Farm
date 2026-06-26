package com.zsj.amethystfarm.client.render;

import com.zsj.amethystfarm.network.SettingsSyncPayload;

public final class ClientSettingsBridge {
    private ClientSettingsBridge() {}

    public static void apply(SettingsSyncPayload payload) {
        ClientRenderConfig.maxRenderDistance = payload.maxRenderDistance();
        ClientRenderConfig.maxCrystalOutlines = payload.maxCrystalOutlines();
    }
}
