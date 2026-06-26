package com.zsj.amethystfarm.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class NbtCompat {
    private NbtCompat() {}

    public static void readString(CompoundTag tag, String key, Consumer<String> consumer) {
        //? if >=1.21.5 {
        /*tag.getString(key).ifPresent(consumer);
        *///?} else {
        if (tag.contains(key)) {
            consumer.accept(tag.getString(key));
        }
        //?}
    }

    public static void readLong(CompoundTag tag, String key, LongConsumer consumer) {
        //? if >=1.21.5 {
        /*tag.getLong(key).ifPresent(value -> consumer.accept(value));
        *///?} else {
        if (tag.contains(key)) {
            consumer.accept(tag.getLong(key));
        }
        //?}
    }

    public static boolean readBoolean(CompoundTag tag, String key, boolean defaultValue) {
        //? if >=1.21.5 {
        /*return tag.getBoolean(key).orElse(defaultValue);
        *///?} else {
        return tag.contains(key) ? tag.getBoolean(key) : defaultValue;
         //?}
    }

    public static void readCompound(CompoundTag tag, String key, Consumer<CompoundTag> consumer) {
        //? if >=1.21.5 {
        /*tag.getCompound(key).ifPresent(consumer);
        *///?} else {
        if (tag.contains(key)) {
            consumer.accept(tag.getCompound(key));
        }
        //?}
    }

    public static CompoundTag readCompoundOrEmpty(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getCompound(key).orElse(new CompoundTag());
        *///?} else {
        return tag.contains(key) ? tag.getCompound(key) : new CompoundTag();
         //?}
    }

    public static Set<String> tagKeys(CompoundTag tag) {
        //? if >=1.21.5 {
        /*return tag.keySet();
        *///?} else {
        return tag.getAllKeys();
         //?}
    }

    public static int worldTopY(ServerLevel level) {
        //? if >=1.21.5 {
        /*return level.getMaxY();
        *///?} else {
        return level.dimensionType().height() - 1;
         //?}
    }

    public static Direction directionFromDelta(double x, double y, double z) {
        //? if >=1.21.4 {
        /*return Direction.getApproximateNearest(x, y, z);
        *///?} else {
        double ax = Math.abs(x);
        double ay = Math.abs(y);
        double az = Math.abs(z);
        if (ax > ay && ax > az) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
        if (ay > az) {
            return y > 0 ? Direction.UP : Direction.DOWN;
        }
        return z > 0 ? Direction.SOUTH : Direction.NORTH;
         //?}
    }
}
