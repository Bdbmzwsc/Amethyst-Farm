package com.zsj.amethystfarm.client;

import com.zsj.amethystfarm.client.gui.AmethystFarmScreen;
import com.zsj.amethystfarm.client.preview.ClientPreviewCache;
import com.zsj.amethystfarm.gui.AmethystFarmMenuTypes;
import com.zsj.amethystfarm.client.render.ClientSettingsBridge;
import com.zsj.amethystfarm.network.MiningTargetPayload;
import com.zsj.amethystfarm.network.PreviewSyncPayload;
import com.zsj.amethystfarm.network.SettingsSyncPayload;
import com.zsj.amethystfarm.client.preview.ClientMiningCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class AmethystFarmClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(AmethystFarmMenuTypes.AMETHYST_FARM_MENU, AmethystFarmScreen::new);
        AmethystPreviewRenderer.register();
        ClientPlayNetworking.registerGlobalReceiver(PreviewSyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> ClientPreviewCache.apply(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(MiningTargetPayload.TYPE, (payload, context) ->
            context.client().execute(() -> ClientMiningCache.apply(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(SettingsSyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> ClientSettingsBridge.apply(payload))
        );
    }
}
