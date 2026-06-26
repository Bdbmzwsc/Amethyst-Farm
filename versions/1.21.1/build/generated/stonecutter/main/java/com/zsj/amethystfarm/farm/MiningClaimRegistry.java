package com.zsj.amethystfarm.farm;

import com.zsj.amethystfarm.config.AmethystFarmSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MiningClaimRegistry {
    private record ClaimKey(ResourceKey<Level> dimension, long blockPos) {}

    private static final Map<ClaimKey, UUID> CLAIMS = new ConcurrentHashMap<>();
    private static final Map<UUID, ClaimKey> ACTIVE_CLAIM_BY_BOT = new ConcurrentHashMap<>();

    private MiningClaimRegistry() {}

    public static List<BlockPos> filterAvailable(ServerPlayer bot, List<BlockPos> crystals) {
        if (!AmethystFarmSettings.multiBotMining) {
            return crystals;
        }
        ResourceKey<Level> dimension = bot.level().dimension();
        return crystals.stream()
            .filter(pos -> isAvailable(dimension, bot.getUUID(), pos))
            .toList();
    }

    public static boolean isAvailable(ResourceKey<Level> dimension, UUID botId, BlockPos pos) {
        if (!AmethystFarmSettings.multiBotMining) {
            return true;
        }
        UUID owner = CLAIMS.get(new ClaimKey(dimension, pos.asLong()));
        return owner == null || owner.equals(botId);
    }

    public static boolean tryClaim(ServerPlayer bot, BlockPos pos) {
        if (!AmethystFarmSettings.multiBotMining) {
            release(bot);
            return true;
        }
        ResourceKey<Level> dimension = bot.level().dimension();
        ClaimKey key = new ClaimKey(dimension, pos.asLong());
        UUID botId = bot.getUUID();

        release(bot);

        UUID existing = CLAIMS.get(key);
        if (existing != null && !existing.equals(botId)) {
            return false;
        }

        CLAIMS.put(key, botId);
        ACTIVE_CLAIM_BY_BOT.put(botId, key);
        return true;
    }

    public static void release(ServerPlayer bot) {
        if (!AmethystFarmSettings.multiBotMining) {
            return;
        }
        ClaimKey key = ACTIVE_CLAIM_BY_BOT.remove(bot.getUUID());
        if (key != null) {
            CLAIMS.remove(key, bot.getUUID());
        }
    }

    public static void releaseAllForBot(UUID botId) {
        ClaimKey key = ACTIVE_CLAIM_BY_BOT.remove(botId);
        if (key != null) {
            CLAIMS.remove(key, botId);
        }
    }

    public static int activeClaimCount() {
        return CLAIMS.size();
    }
}
