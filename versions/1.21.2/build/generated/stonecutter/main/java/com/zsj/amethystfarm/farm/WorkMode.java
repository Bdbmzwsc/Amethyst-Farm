package com.zsj.amethystfarm.farm;

public enum WorkMode {
    IDLE("空闲", "假人不执行任何农场操作"),
    SCAN("扫描紫水晶", "预览假人附近范围内的紫水晶"),
    HARVEST("挖掘附近紫水晶", "自动采集假人手长范围内的可挖掘紫水晶"),
    LOCK_MINE("单点锁定挖掘", "对准视线内最近的可挖掘紫水晶并挖掘");

    private final String displayName;
    private final String description;

    WorkMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static WorkMode fromString(String name) {
        for (WorkMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name) || mode.displayName.equals(name)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown work mode: " + name);
    }
}
