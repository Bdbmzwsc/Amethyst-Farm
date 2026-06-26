package com.zsj.amethystfarm.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record AmethystFarmMenuData(UUID fakePlayerId) {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystFarmMenuData> STREAM_CODEC = StreamCodec.of(
        (buf, data) -> buf.writeUUID(data.fakePlayerId()),
        buf -> new AmethystFarmMenuData(buf.readUUID())
    );
}
