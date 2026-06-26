package com.zsj.amethystfarm.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import com.zsj.amethystfarm.util.ModCompat;
import com.zsj.amethystfarm.util.ModIds;
import com.zsj.amethystfarm.util.NbtCompat;
import net.minecraft.world.level.Level;

public class AmethystFarmBinding {
    public static final AmethystFarmBinding EMPTY = new AmethystFarmBinding();

    private BlockPos pos1 = BlockPos.ZERO;
    private BlockPos pos2 = BlockPos.ZERO;
    private boolean bound;
    private ResourceKey<Level> dimension;

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public boolean isBound() {
        return bound;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public void setCorner1(BlockPos pos, ResourceKey<Level> dimension) {
        this.pos1 = pos.immutable();
        this.dimension = dimension;
        updateBound();
    }

    public void setCorner2(BlockPos pos, ResourceKey<Level> dimension) {
        this.pos2 = pos.immutable();
        this.dimension = dimension;
        updateBound();
    }

    public void setRange(BlockPos from, BlockPos to, ResourceKey<Level> dimension) {
        this.pos1 = from.immutable();
        this.pos2 = to.immutable();
        this.dimension = dimension;
        updateBound();
    }

    public void clear() {
        pos1 = BlockPos.ZERO;
        pos2 = BlockPos.ZERO;
        bound = false;
        dimension = null;
    }

    public BlockPos getMin() {
        return new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ())
        );
    }

    public BlockPos getMax() {
        return new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ())
        );
    }

    public long volume() {
        if (!bound) {
            return 0;
        }
        BlockPos min = getMin();
        BlockPos max = getMax();
        return (long) (max.getX() - min.getX() + 1)
            * (max.getY() - min.getY() + 1)
            * (max.getZ() - min.getZ() + 1);
    }

    private void updateBound() {
        bound = !pos1.equals(BlockPos.ZERO) || !pos2.equals(BlockPos.ZERO);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("pos1", pos1.asLong());
        tag.putLong("pos2", pos2.asLong());
        tag.putBoolean("bound", bound);
        if (dimension != null) {
            tag.putString("dimension", ModCompat.dimensionKey(dimension));
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        NbtCompat.readLong(tag, "pos1", value -> pos1 = BlockPos.of(value));
        NbtCompat.readLong(tag, "pos2", value -> pos2 = BlockPos.of(value));
        bound = NbtCompat.readBoolean(tag, "bound", false);
        NbtCompat.readString(tag, "dimension", value ->
            dimension = ResourceKey.create(Registries.DIMENSION, ModIds.parse(value))
        );
    }

    public void copyFrom(AmethystFarmBinding source) {
        load(source.save());
    }

    /** Builds a cubic scan bounds centered on {@code center} (used for nearby preview/sync). */
    public static AmethystFarmBinding around(BlockPos center, int radius, ResourceKey<Level> dimension) {
        AmethystFarmBinding binding = new AmethystFarmBinding();
        binding.setRange(
            center.offset(-radius, -radius, -radius),
            center.offset(radius, radius, radius),
            dimension
        );
        return binding;
    }
}
