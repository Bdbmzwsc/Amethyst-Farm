package com.zsj.amethystfarm.farm;

import com.zsj.amethystfarm.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class HarvestWhitelist {
    private boolean smallBud = true;
    private boolean mediumBud = true;
    private boolean largeBud = true;
    private boolean cluster = true;
    /** When true, only harvest crystals on horizontal faces of budding amethyst (not top/bottom). */
    private boolean sidesOnly = true;

    public boolean isSmallBud() {
        return smallBud;
    }

    public void setSmallBud(boolean enabled) {
        this.smallBud = enabled;
    }

    public void toggleSmallBud() {
        smallBud = !smallBud;
    }

    public boolean isMediumBud() {
        return mediumBud;
    }

    public void setMediumBud(boolean enabled) {
        this.mediumBud = enabled;
    }

    public void toggleMediumBud() {
        mediumBud = !mediumBud;
    }

    public boolean isLargeBud() {
        return largeBud;
    }

    public void setLargeBud(boolean enabled) {
        this.largeBud = enabled;
    }

    public void toggleLargeBud() {
        largeBud = !largeBud;
    }

    public boolean isCluster() {
        return cluster;
    }

    public void setCluster(boolean enabled) {
        this.cluster = enabled;
    }

    public void toggleCluster() {
        cluster = !cluster;
    }

    public boolean isSidesOnly() {
        return sidesOnly;
    }

    public void setSidesOnly(boolean enabled) {
        this.sidesOnly = enabled;
    }

    public void toggleSidesOnly() {
        sidesOnly = !sidesOnly;
    }

    public boolean hasAnyTypeEnabled() {
        return smallBud || mediumBud || largeBud || cluster;
    }

    public Component formatSummary() {
        StringBuilder sb = new StringBuilder();
        if (smallBud) {
            sb.append("小芽");
        }
        if (mediumBud) {
            if (!sb.isEmpty()) {
                sb.append("/");
            }
            sb.append("中芽");
        }
        if (largeBud) {
            if (!sb.isEmpty()) {
                sb.append("/");
            }
            sb.append("大芽");
        }
        if (cluster) {
            if (!sb.isEmpty()) {
                sb.append("/");
            }
            sb.append("簇");
        }
        if (sb.isEmpty()) {
            sb.append("无");
        }
        if (sidesOnly) {
            sb.append(" | 仅侧面");
        }
        return Component.literal(sb.toString());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("smallBud", smallBud);
        tag.putBoolean("mediumBud", mediumBud);
        tag.putBoolean("largeBud", largeBud);
        tag.putBoolean("cluster", cluster);
        tag.putBoolean("sidesOnly", sidesOnly);
        return tag;
    }

    public void load(CompoundTag tag) {
        smallBud = NbtCompat.readBoolean(tag, "smallBud", true);
        mediumBud = NbtCompat.readBoolean(tag, "mediumBud", true);
        largeBud = NbtCompat.readBoolean(tag, "largeBud", true);
        cluster = NbtCompat.readBoolean(tag, "cluster", true);
        sidesOnly = NbtCompat.readBoolean(tag, "sidesOnly", true);
    }
}
