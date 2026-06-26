package com.zsj.amethystfarm.util;

public final class ModIds {
    private ModIds() {}

    //? if >=1.21.11 {
    public static net.minecraft.resources.Identifier id(String namespace, String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static net.minecraft.resources.Identifier parse(String value) {
        return net.minecraft.resources.Identifier.parse(value);
    }
    //?} else {
    /*public static net.minecraft.resources.ResourceLocation id(String namespace, String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static net.minecraft.resources.ResourceLocation parse(String value) {
        return net.minecraft.resources.ResourceLocation.parse(value);
    }
    *///?}
}
