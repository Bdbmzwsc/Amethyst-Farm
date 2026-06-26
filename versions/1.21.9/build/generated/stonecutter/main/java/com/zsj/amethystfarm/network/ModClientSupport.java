package com.zsj.amethystfarm.network;

import com.zsj.amethystfarm.AmethystFarmMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Detects whether a player has the Amethyst Farm client mod installed. */
public final class ModClientSupport {
    private ModClientSupport() {}

    public static boolean hasClientMod(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, SettingsSyncPayload.TYPE);
    }

    public static void requireClientModOrNotify(ServerPlayer player) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "此功能需要客户端安装 " + AmethystFarmMod.MOD_ID + " 模组（GUI / 预览）。"
                + "无模组玩家仍可使用 /af 命令控制假人。"
        ));
    }
}
