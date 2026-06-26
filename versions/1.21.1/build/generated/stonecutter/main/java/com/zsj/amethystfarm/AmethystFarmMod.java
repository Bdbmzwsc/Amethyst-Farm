package com.zsj.amethystfarm;

import com.zsj.amethystfarm.CarpetHooks;
import com.zsj.amethystfarm.command.AmethystFarmCommands;
import com.zsj.amethystfarm.data.AmethystFarmDataManager;
import com.zsj.amethystfarm.gui.AmethystFarmMenus;
import com.zsj.amethystfarm.network.AmethystFarmNetworking;
import com.zsj.amethystfarm.farm.MiningClaimRegistry;
import com.zsj.amethystfarm.network.SettingsSyncHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmethystFarmMod implements ModInitializer {
    public static final String MOD_ID = "amethystfarm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AmethystFarmMenus.register();
        AmethystFarmNetworking.registerPayloadTypes();
        AmethystFarmDataManager.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            AmethystFarmCommands.register(dispatcher)
        );
        ServerLifecycleEvents.SERVER_STARTED.register(server -> AmethystFarmDataManager.load(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(AmethystFarmDataManager::save);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (CarpetHooks.isFakePlayer(player)) {
                AmethystFarmDataManager.onFakePlayerLeave(player);
                MiningClaimRegistry.releaseAllForBot(player.getUUID());
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            AmethystFarmDataManager.onFakePlayerJoin(player);
            SettingsSyncHelper.sendToPlayer(player);
        });
        if (!AmethystFarmRuntime.isCarpetLoaded()) {
            LOGGER.warn("未检测到 Carpet Mod：假人农场逻辑已禁用，仅保留客户端预览/GUI 能力");
        } else {
            LOGGER.info("Amethyst Farm Bot initialized");
        }
    }
}
