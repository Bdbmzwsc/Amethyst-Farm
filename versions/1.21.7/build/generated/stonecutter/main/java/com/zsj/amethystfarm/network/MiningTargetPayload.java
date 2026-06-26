package com.zsj.amethystfarm.network;

import com.zsj.amethystfarm.AmethystFarmMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.zsj.amethystfarm.util.ModIds;

public record MiningTargetPayload(byte action, int fakeEntityId, BlockPos target) implements CustomPacketPayload {
    public static final byte CLEAR = 0;
    public static final byte SET = 1;

    public static final CustomPacketPayload.Type<MiningTargetPayload> TYPE =
        new CustomPacketPayload.Type<>(ModIds.id(AmethystFarmMod.MOD_ID, "mining_target"));

    public static final StreamCodec<FriendlyByteBuf, MiningTargetPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeByte(payload.action());
            buf.writeVarInt(payload.fakeEntityId());
            if (payload.action() == SET) {
                BlockPosNetwork.writeBlockPos(buf, payload.target());
            }
        },
        buf -> {
            byte action = buf.readByte();
            int entityId = buf.readVarInt();
            BlockPos target = action == SET ? BlockPosNetwork.readBlockPos(buf) : BlockPos.ZERO;
            return new MiningTargetPayload(action, entityId, target);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static MiningTargetPayload of(int fakeEntityId, BlockPos target) {
        return new MiningTargetPayload(SET, fakeEntityId, target);
    }

    public static MiningTargetPayload cleared(int fakeEntityId) {
        return new MiningTargetPayload(CLEAR, fakeEntityId, BlockPos.ZERO);
    }
}
