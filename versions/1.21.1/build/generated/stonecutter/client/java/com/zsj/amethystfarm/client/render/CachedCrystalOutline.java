package com.zsj.amethystfarm.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CachedCrystalOutline(BlockPos pos, AABB localBounds, boolean mature) {
    public AABB worldBounds() {
        return localBounds.move(pos);
    }

    public double distanceSquaredTo(Vec3 point) {
        return worldBounds().getCenter().distanceToSqr(point);
    }
}
