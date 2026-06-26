package com.zsj.amethystfarm.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class ModCompat {
    private ModCompat() {}

    public static String profileName(GameProfile profile) {
        //? if >=1.21.9 {
        /*return profile.name();
        *///?} else {
        return profile.getName();
         //?}
    }

    public static MinecraftServer serverOf(ServerPlayer player) {
        //? if >=1.21.9 {
        /*return player.level().getServer();
        *///?} else {
        return player.getServer();
         //?}
    }

    public static String dimensionKey(ResourceKey<Level> dimension) {
        //? if >=1.21.11 {
        /*return dimension.identifier().toString();
        *///?} else {
        return dimension.location().toString();
         //?}
    }
}
