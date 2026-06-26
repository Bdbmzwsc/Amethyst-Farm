package com.zsj.amethystfarm.data;

import com.zsj.amethystfarm.AmethystFarmMod;
import com.zsj.amethystfarm.CarpetHooks;
import com.zsj.amethystfarm.config.AmethystFarmSettings;
import com.zsj.amethystfarm.fakes.AmethystFarmBotAccess;
import com.zsj.amethystfarm.farm.AmethystFarmProfile;
import com.zsj.amethystfarm.util.ModCompat;
import com.zsj.amethystfarm.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AmethystFarmDataManager {
    private static final String BY_NAME_KEY = "byName";

    private static final Map<UUID, AmethystFarmProfile> PROFILES = new HashMap<>();
    private static final Map<String, AmethystFarmProfile> PROFILES_BY_NAME = new HashMap<>();
    /** v1 saves keyed by UUID, merged into byName when matching fake joins. */
    private static final Map<UUID, AmethystFarmProfile> LEGACY_UUID_PROFILES = new HashMap<>();

    private AmethystFarmDataManager() {}

    public static void init() {}

    public static AmethystFarmProfile getOrCreate(ServerPlayer player) {
        AmethystFarmProfile profile = PROFILES.get(player.getUUID());
        if (profile != null) {
            return profile;
        }
        return resolveProfile(player);
    }

    public static AmethystFarmProfile get(ServerPlayer player) {
        if (player instanceof AmethystFarmBotAccess access) {
            return access.amethystfarm$getProfile();
        }
        return getOrCreate(player);
    }

    public static void onFakePlayerJoin(ServerPlayer player) {
        if (!CarpetHooks.isFakePlayer(player)) {
            return;
        }
        // Only restore a saved profile; never create one on join (avoids touching unrelated fakes).
        String name = ModCompat.profileName(player.getGameProfile());
        AmethystFarmProfile saved = PROFILES_BY_NAME.get(name);
        if (saved != null) {
            PROFILES.put(player.getUUID(), saved);
        }
    }

    /** True when this fake is under farm control and may tick harvest logic. */
    public static boolean isFarmActive(ServerPlayer player) {
        if (!CarpetHooks.isFakePlayer(player)) {
            return false;
        }
        AmethystFarmProfile profile = PROFILES.get(player.getUUID());
        if (profile == null) {
            profile = PROFILES_BY_NAME.get(ModCompat.profileName(player.getGameProfile()));
        }
        return profile != null && profile.isBotEnabled();
    }

    public static void onFakePlayerLeave(ServerPlayer player) {
        if (!CarpetHooks.isFakePlayer(player)) {
            return;
        }
        String name = ModCompat.profileName(player.getGameProfile());
        AmethystFarmProfile profile = PROFILES.get(player.getUUID());
        if (profile != null) {
            PROFILES_BY_NAME.put(name, profile);
        }
        PROFILES.remove(player.getUUID());
    }

    private static AmethystFarmProfile resolveProfile(ServerPlayer player) {
        UUID uuid = player.getUUID();
        String name = ModCompat.profileName(player.getGameProfile());

        AmethystFarmProfile profile = PROFILES_BY_NAME.get(name);
        if (profile == null) {
            profile = LEGACY_UUID_PROFILES.remove(uuid);
        }
        if (profile == null) {
            profile = new AmethystFarmProfile();
        }

        if (CarpetHooks.isFakePlayer(player)) {
            PROFILES_BY_NAME.put(name, profile);
        }
        PROFILES.put(uuid, profile);
        return profile;
    }

    public static void syncOnlineFakeProfiles(MinecraftServer server) {
        for (ServerPlayer player : CarpetHooks.getOnlineFakePlayers(server)) {
            String name = ModCompat.profileName(player.getGameProfile());
            AmethystFarmProfile profile = get(player);
            PROFILES_BY_NAME.put(name, profile);
            PROFILES.put(player.getUUID(), profile);
        }
    }

    public static Map<String, AmethystFarmProfile> getProfilesByName() {
        return PROFILES_BY_NAME;
    }

    public static void save(MinecraftServer server) {
        saveProfiles(server);
        saveSettings(server);
    }

    public static void persistProfiles(MinecraftServer server) {
        if (server != null) {
            syncOnlineFakeProfiles(server);
            saveProfiles(server);
        }
    }

    public static void load(MinecraftServer server) {
        loadSettings(server);
        loadProfiles(server);
    }

    private static void saveProfiles(MinecraftServer server) {
        syncOnlineFakeProfiles(server);
        CompoundTag root = new CompoundTag();
        CompoundTag byName = new CompoundTag();
        PROFILES_BY_NAME.forEach((name, profile) -> byName.put(name, profile.save()));
        root.put(BY_NAME_KEY, byName);
        try {
            Path path = profilesPath(server);
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(root, path);
        } catch (IOException e) {
            AmethystFarmMod.LOGGER.error("Failed to save amethyst farm profiles", e);
        }
    }

    private static void loadProfiles(MinecraftServer server) {
        PROFILES.clear();
        PROFILES_BY_NAME.clear();
        LEGACY_UUID_PROFILES.clear();
        Path path = profilesPath(server);
        if (!Files.exists(path)) {
            return;
        }
        try {
            CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            if (root.contains(BY_NAME_KEY)) {
                CompoundTag byName = NbtCompat.readCompoundOrEmpty(root, BY_NAME_KEY);
                for (String name : NbtCompat.tagKeys(byName)) {
                    AmethystFarmProfile profile = new AmethystFarmProfile();
                    NbtCompat.readCompound(byName, name, profile::load);
                    PROFILES_BY_NAME.put(name, profile);
                }
            } else {
                loadLegacyUuidProfiles(root);
            }
        } catch (IOException e) {
            AmethystFarmMod.LOGGER.error("Failed to load amethyst farm profiles", e);
        }

        for (ServerPlayer player : CarpetHooks.getOnlineFakePlayers(server)) {
            onFakePlayerJoin(player);
        }
    }

    private static void loadLegacyUuidProfiles(CompoundTag root) {
        for (String key : NbtCompat.tagKeys(root)) {
            try {
                UUID uuid = UUID.fromString(key);
                AmethystFarmProfile profile = new AmethystFarmProfile();
                NbtCompat.readCompound(root, key, profile::load);
                LEGACY_UUID_PROFILES.put(uuid, profile);
            } catch (IllegalArgumentException ignored) {
                // not a UUID key
            }
        }
        if (!LEGACY_UUID_PROFILES.isEmpty()) {
            AmethystFarmMod.LOGGER.info(
                "Loaded {} legacy UUID profile(s); will restore when matching fake player joins",
                LEGACY_UUID_PROFILES.size()
            );
        }
    }

    private static void saveSettings(MinecraftServer server) {
        try {
            Path path = settingsPath(server);
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(AmethystFarmSettings.save(), path);
        } catch (IOException e) {
            AmethystFarmMod.LOGGER.error("Failed to save amethyst farm settings", e);
        }
    }

    private static void loadSettings(MinecraftServer server) {
        Path path = settingsPath(server);
        if (!Files.exists(path)) {
            return;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            AmethystFarmSettings.load(tag);
        } catch (IOException e) {
            AmethystFarmMod.LOGGER.error("Failed to load amethyst farm settings", e);
        }
    }

    private static Path profilesPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/amethystfarm_profiles.dat");
    }

    private static Path settingsPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/amethystfarm_settings.dat");
    }
}
