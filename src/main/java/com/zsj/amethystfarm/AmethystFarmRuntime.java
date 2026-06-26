package com.zsj.amethystfarm;

import net.fabricmc.loader.api.FabricLoader;

public final class AmethystFarmRuntime {
    private AmethystFarmRuntime() {}

    public static boolean isCarpetLoaded() {
        return FabricLoader.getInstance().isModLoaded("carpet");
    }

    public static boolean isServerLogicEnabled() {
        return isCarpetLoaded();
    }
}
