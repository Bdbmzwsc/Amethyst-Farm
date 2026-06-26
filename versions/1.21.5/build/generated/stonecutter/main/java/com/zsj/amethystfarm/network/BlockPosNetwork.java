package com.zsj.amethystfarm.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public final class BlockPosNetwork {
    private BlockPosNetwork() {}

    public static void writeBlockPos(FriendlyByteBuf buf, BlockPos pos) {
        buf.writeVarInt(pos.getX());
        buf.writeVarInt(pos.getY());
        buf.writeVarInt(pos.getZ());
    }

    public static BlockPos readBlockPos(FriendlyByteBuf buf) {
        return new BlockPos(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void writeRelative(FriendlyByteBuf buf, BlockPos origin, BlockPos pos) {
        buf.writeVarInt(pos.getX() - origin.getX());
        buf.writeVarInt(pos.getY() - origin.getY());
        buf.writeVarInt(pos.getZ() - origin.getZ());
    }

    public static BlockPos readRelative(FriendlyByteBuf buf, BlockPos origin) {
        return origin.offset(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
}
