package com.zsj.amethystfarm.network;

import com.zsj.amethystfarm.AmethystFarmMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.zsj.amethystfarm.util.ModIds;

import java.util.ArrayList;
import java.util.List;

public record PreviewSyncPayload(
    byte action,
    int fakeEntityId,
    BlockPos origin,
    BlockPos extent,
    List<CrystalEntry> added,
    List<BlockPos> removedRelative
) implements CustomPacketPayload {
    public static final byte CLEAR = 0;
    public static final byte FULL = 1;
    public static final byte DELTA = 2;

    public static final CustomPacketPayload.Type<PreviewSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(ModIds.id(AmethystFarmMod.MOD_ID, "preview_sync"));

    public record CrystalEntry(int dx, int dy, int dz, boolean mature) {}

    public static final StreamCodec<FriendlyByteBuf, PreviewSyncPayload> CODEC = StreamCodec.of(
        PreviewSyncPayload::encode,
        PreviewSyncPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static PreviewSyncPayload cleared(int fakeEntityId) {
        return new PreviewSyncPayload(CLEAR, fakeEntityId, BlockPos.ZERO, BlockPos.ZERO, List.of(), List.of());
    }

    public static PreviewSyncPayload full(int fakeEntityId, BlockPos origin, BlockPos extent, List<CrystalEntry> crystals) {
        return new PreviewSyncPayload(FULL, fakeEntityId, origin, extent, crystals, List.of());
    }

    public static PreviewSyncPayload delta(
        int fakeEntityId,
        BlockPos origin,
        List<CrystalEntry> added,
        List<BlockPos> removedRelative
    ) {
        return new PreviewSyncPayload(DELTA, fakeEntityId, origin, BlockPos.ZERO, added, removedRelative);
    }

    private static void encode(FriendlyByteBuf buf, PreviewSyncPayload payload) {
        buf.writeByte(payload.action());
        buf.writeVarInt(payload.fakeEntityId());
        switch (payload.action()) {
            case FULL -> {
                BlockPosNetwork.writeBlockPos(buf, payload.origin());
                BlockPosNetwork.writeBlockPos(buf, payload.extent());
                writeEntries(buf, payload.added());
            }
            case DELTA -> {
                BlockPosNetwork.writeBlockPos(buf, payload.origin());
                writeRelativeList(buf, payload.removedRelative());
                writeEntries(buf, payload.added());
            }
            default -> {}
        }
    }

    private static PreviewSyncPayload decode(FriendlyByteBuf buf) {
        byte action = buf.readByte();
        int entityId = buf.readVarInt();
        return switch (action) {
            case FULL -> new PreviewSyncPayload(
                FULL,
                entityId,
                BlockPosNetwork.readBlockPos(buf),
                BlockPosNetwork.readBlockPos(buf),
                readEntries(buf),
                List.of()
            );
            case DELTA -> {
                BlockPos origin = BlockPosNetwork.readBlockPos(buf);
                List<BlockPos> removed = readRelativeList(buf);
                List<CrystalEntry> added = readEntries(buf);
                yield new PreviewSyncPayload(DELTA, entityId, origin, BlockPos.ZERO, added, removed);
            }
            default -> cleared(entityId);
        };
    }

    private static void writeEntries(FriendlyByteBuf buf, List<CrystalEntry> entries) {
        buf.writeVarInt(entries.size());
        for (CrystalEntry entry : entries) {
            buf.writeVarInt(entry.dx());
            buf.writeVarInt(entry.dy());
            buf.writeVarInt(entry.dz());
            buf.writeBoolean(entry.mature());
        }
    }

    private static List<CrystalEntry> readEntries(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<CrystalEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new CrystalEntry(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean()
            ));
        }
        return entries;
    }

    private static void writeRelativeList(FriendlyByteBuf buf, List<BlockPos> relative) {
        buf.writeVarInt(relative.size());
        for (BlockPos rel : relative) {
            buf.writeVarInt(rel.getX());
            buf.writeVarInt(rel.getY());
            buf.writeVarInt(rel.getZ());
        }
    }

    private static List<BlockPos> readRelativeList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<BlockPos> relative = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            relative.add(new BlockPos(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return relative;
    }
}
