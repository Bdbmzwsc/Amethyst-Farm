package com.zsj.amethystfarm.gui;

import com.zsj.amethystfarm.AmethystFarmMod;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.registries.BuiltInRegistries;
import com.zsj.amethystfarm.network.ModClientSupport;
import net.minecraft.network.chat.Component;
import com.zsj.amethystfarm.util.ModIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class AmethystFarmMenus {
    private AmethystFarmMenus() {}

    public static void register() {
        AmethystFarmMenuTypes.AMETHYST_FARM_MENU = net.minecraft.core.Registry.register(
            BuiltInRegistries.MENU,
            ModIds.id(AmethystFarmMod.MOD_ID, "amethyst_farm"),
            new ExtendedScreenHandlerType<>(
                (syncId, inv, data) -> new AmethystFarmMenu(syncId, inv, data.fakePlayerId()),
                AmethystFarmMenuData.STREAM_CODEC
            )
        );
    }

    public static void open(ServerPlayer operator, ServerPlayer fakePlayer) {
        if (!ModClientSupport.hasClientMod(operator)) {
            ModClientSupport.requireClientModOrNotify(operator);
            return;
        }
        operator.openMenu(new ExtendedScreenHandlerFactory<AmethystFarmMenuData>() {
            @Override
            public AmethystFarmMenuData getScreenOpeningData(ServerPlayer player) {
                return new AmethystFarmMenuData(fakePlayer.getUUID());
            }

            @Override
            public Component getDisplayName() {
                return Component.literal("紫水晶农场 — " + fakePlayer.getName().getString());
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                return new AmethystFarmMenu(syncId, inv, fakePlayer.getUUID());
            }
        });
        if (operator.containerMenu instanceof AmethystFarmMenu menu) {
            menu.syncFromProfile(operator);
        }
    }
}
