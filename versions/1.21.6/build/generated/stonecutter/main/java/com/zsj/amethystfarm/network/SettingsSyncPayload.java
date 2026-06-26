package com.zsj.amethystfarm.network;

import com.zsj.amethystfarm.AmethystFarmMod;
import com.zsj.amethystfarm.config.AmethystFarmSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.zsj.amethystfarm.util.ModIds;

public record SettingsSyncPayload(float maxRenderDistance, int maxCrystalOutlines) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SettingsSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(ModIds.id(AmethystFarmMod.MOD_ID, "settings_sync"));

    public static final StreamCodec<FriendlyByteBuf, SettingsSyncPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeFloat(payload.maxRenderDistance());
            buf.writeVarInt(payload.maxCrystalOutlines());
        },
        buf -> new SettingsSyncPayload(buf.readFloat(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static SettingsSyncPayload fromSettings() {
        return new SettingsSyncPayload(
            (float) AmethystFarmSettings.maxRenderDistance,
            AmethystFarmSettings.maxCrystalOutlines
        );
    }
}
