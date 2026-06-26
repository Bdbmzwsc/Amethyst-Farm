package com.zsj.amethystfarm.config;

import com.zsj.amethystfarm.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AmethystFarmSettings {
    public static boolean enabled = true;
    public static boolean autoHarvest = true;
    public static boolean scanPreview = true;
    public static int scanInterval = 20;
    public static int scanRadius = 8;
    public static int maxScanVolume = 8000;
    public static double miningReach = 4.5;
    public static boolean multiBotMining = true;
    public static int previewCrystalLimit = 96;
    public static double maxRenderDistance = 32.0;
    public static int maxCrystalOutlines = 48;

    private static final Map<String, RuleDefinition> RULES = new LinkedHashMap<>();

    static {
        registerBoolean("enabled", "启用紫水晶农场假人功能", () -> enabled, v -> enabled = v);
        registerBoolean("autoHarvest", "假人自动扫描并采集紫水晶", () -> autoHarvest, v -> autoHarvest = v);
        registerBoolean("scanPreview", "扫描模式显示范围与紫水晶预览描边", () -> scanPreview, v -> scanPreview = v);
        registerBoolean("multiBotMining", "多假人并行挖掘（自动分配不同紫水晶，避免抢挖）", () -> multiBotMining, v -> multiBotMining = v);
        registerInteger("scanInterval", "假人扫描间隔（游戏刻）", () -> scanInterval, v -> scanInterval = v, 1, Integer.MAX_VALUE);
        registerInteger("scanRadius", "假人附近扫描半径（方块，以假人为中心）", () -> scanRadius, v -> scanRadius = v, 1, 32);
        registerInteger("maxScanVolume", "单次扫描最大方块数", () -> maxScanVolume, v -> maxScanVolume = v, 1, Integer.MAX_VALUE);
        registerInteger("previewCrystalLimit", "同步到客户端的预览紫水晶数量上限", () -> previewCrystalLimit, v -> previewCrystalLimit = v, 16, 512);
        registerInteger("maxCrystalOutlines", "客户端最多绘制的紫水晶描边数", () -> maxCrystalOutlines, v -> maxCrystalOutlines = v, 8, 256);
        registerDouble("miningReach", "假人挖掘距离（手长，方块）", () -> miningReach, v -> miningReach = v, 1.0, 6.0);
        registerDouble("maxRenderDistance", "客户端描边最大渲染距离（方块）", () -> maxRenderDistance, v -> maxRenderDistance = v, 8.0, 128.0);
    }

    private AmethystFarmSettings() {}

    public static List<String> ruleNames() {
        return RULES.values().stream().map(RuleDefinition::id).toList();
    }

    public static Optional<RuleDefinition> findRule(String name) {
        return Optional.ofNullable(RULES.get(normalize(name)));
    }

    public static String getValue(String name) {
        RuleDefinition rule = RULES.get(normalize(name));
        if (rule == null) {
            throw new IllegalArgumentException("未知规则: " + name);
        }
        return rule.get();
    }

    public static void setValue(String name, String rawValue) {
        RuleDefinition rule = RULES.get(normalize(name));
        if (rule == null) {
            throw new IllegalArgumentException("未知规则: " + name);
        }
        rule.set(rawValue);
    }

    public static CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (RuleDefinition rule : RULES.values()) {
            tag.putString(rule.id(), rule.get());
        }
        return tag;
    }

    public static void load(CompoundTag tag) {
        for (RuleDefinition rule : RULES.values()) {
            NbtCompat.readString(tag, rule.id(), rule::set);
        }
    }

    private static String normalize(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "scanvolume" -> "maxscanvolume";
            case "renderdistance" -> "maxrenderdistance";
            case "crystaloutlines" -> "maxcrystaloutlines";
            case "previewlimit" -> "previewcrystallimit";
            default -> key;
        };
    }

    private static void registerBoolean(String id, String description, BooleanGetter getter, BooleanSetter setter) {
        RULES.put(normalize(id), new RuleDefinition(id, description, "true | false", () -> Boolean.toString(getter.get()), value -> {
            boolean parsed = parseBoolean(value);
            setter.set(parsed);
        }));
    }

    private static void registerInteger(String id, String description, IntGetter getter, IntSetter setter, int min, int max) {
        RULES.put(normalize(id), new RuleDefinition(id, description, "整数 (" + min + " ~ " + max + ")", () -> Integer.toString(getter.get()), value -> {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException("数值超出范围: " + min + " ~ " + max);
            }
            setter.set(parsed);
        }));
    }

    private static void registerDouble(String id, String description, DoubleGetter getter, DoubleSetter setter, double min, double max) {
        RULES.put(normalize(id), new RuleDefinition(id, description, "小数 (" + min + " ~ " + max + ")", () -> Double.toString(getter.get()), value -> {
            double parsed = Double.parseDouble(value);
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException("数值超出范围: " + min + " ~ " + max);
            }
            setter.set(parsed);
        }));
    }

    private static boolean parseBoolean(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "on", "yes" -> true;
            case "false", "0", "off", "no" -> false;
            default -> throw new IllegalArgumentException("布尔值应为 true/false");
        };
    }

    @FunctionalInterface
    private interface BooleanGetter {
        boolean get();
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface IntGetter {
        int get();
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int value);
    }

    @FunctionalInterface
    private interface DoubleGetter {
        double get();
    }

    @FunctionalInterface
    private interface DoubleSetter {
        void set(double value);
    }

    @FunctionalInterface
    private interface ValueGetter {
        String get();
    }

    @FunctionalInterface
    private interface ValueSetter {
        void set(String value);
    }

    public record RuleDefinition(String id, String description, String valueHint, ValueGetter getter, ValueSetter setter) {
        public String get() {
            return getter.get();
        }

        public void set(String value) {
            setter.set(value);
        }
    }

    // 兼容旧字段名访问（如有外部引用）
    public static boolean amethystFarmEnabled() {
        return enabled;
    }
}
