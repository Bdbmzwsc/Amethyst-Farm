package com.zsj.amethystfarm.farm;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.zsj.amethystfarm.util.NbtCompat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Breaks a specific block position without ActionPack ATTACK raycasts
 * (which often hit calcite or other blocks in front of amethyst buds).
 */
public final class AmethystBlockBreaker {
    private AmethystBlockBreaker() {}

    public static void abort(ServerPlayer fake, ServerLevel level, AmethystFarmProfile profile) {
        stopAttackAction(fake);
        BlockPos breaking = profile.getBreakingBlockPos();
        if (breaking != null) {
            fake.gameMode.handleBlockBreakAction(
                breaking,
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                Direction.DOWN,
                NbtCompat.worldTopY(level),
                0
            );
            level.destroyBlockProgress(-1, breaking, -1);
        }
        profile.clearBreakingState();
    }

    /**
     * @return true when the target block is gone or breaking finished this tick
     */
    public static boolean tickBreak(
        ServerPlayer fake,
        ServerLevel level,
        AmethystFarmProfile profile,
        BlockPos target,
        HarvestWhitelist whitelist
    ) {
        stopAttackAction(fake);

        BlockState state = level.getBlockState(target);
        if (!AmethystCrystalHelper.matchesHarvestWhitelist(state, level, target, whitelist)) {
            abort(fake, level, profile);
            return true;
        }

        Direction face = breakFace(state, fake, target);
        actionPack(fake).lookAt(lookPoint(state, target));

        if (fake.gameMode.getGameModeForPlayer().isCreative()) {
            fake.gameMode.handleBlockBreakAction(
                target,
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                face,
                NbtCompat.worldTopY(level),
                0
            );
            fake.swing(InteractionHand.MAIN_HAND);
            profile.clearBreakingState();
            return true;
        }

        if (profile.getBlockHitDelay() > 0) {
            profile.setBlockHitDelay(profile.getBlockHitDelay() - 1);
            return false;
        }

        BlockPos breaking = profile.getBreakingBlockPos();
        if (breaking == null || !breaking.equals(target)) {
            if (breaking != null) {
                fake.gameMode.handleBlockBreakAction(
                    breaking,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    face,
                    NbtCompat.worldTopY(level),
                    0
                );
                level.destroyBlockProgress(-1, breaking, -1);
            }
            fake.gameMode.handleBlockBreakAction(
                target,
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                face,
                NbtCompat.worldTopY(level),
                0
            );
            profile.setBreakingBlockPos(target);
            profile.setBlockBreakDamage(0.0f);
            if (!state.isAir()) {
                state.attack(level, target, fake);
            }
            fake.swing(InteractionHand.MAIN_HAND);
            fake.resetLastActionTime();
            return false;
        }

        float progress = state.getDestroyProgress(fake, level, target);
        profile.addBlockBreakDamage(progress);
        level.destroyBlockProgress(-1, target, (int) (profile.getBlockBreakDamage() * 10.0f));
        fake.swing(InteractionHand.MAIN_HAND);
        fake.resetLastActionTime();

        if (profile.getBlockBreakDamage() >= 1.0f) {
            fake.gameMode.handleBlockBreakAction(
                target,
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                face,
                NbtCompat.worldTopY(level),
                0
            );
            level.destroyBlockProgress(-1, target, -1);
            profile.clearBreakingState();
            profile.setBlockHitDelay(5);
            return true;
        }
        return false;
    }

    /** Outward face of attached buds/clusters — avoids aiming into the geode wall. */
    static Vec3 lookPoint(BlockState state, BlockPos target) {
        Vec3 center = Vec3.atCenterOf(target);
        if (state.hasProperty(AmethystClusterBlock.FACING)) {
            Direction outward = state.getValue(AmethystClusterBlock.FACING);
            return center.add(
                outward.getStepX() * 0.48,
                outward.getStepY() * 0.48,
                outward.getStepZ() * 0.48
            );
        }
        return center;
    }

    static Direction breakFace(BlockState state, ServerPlayer fake, BlockPos target) {
        if (state.hasProperty(AmethystClusterBlock.FACING)) {
            return state.getValue(AmethystClusterBlock.FACING);
        }
        Vec3 eye = fake.getEyePosition();
        Vec3 center = Vec3.atCenterOf(target);
        Vec3 delta = center.subtract(eye);
        if (delta.lengthSqr() < 1.0E-6) {
            return Direction.UP;
        }
        return NbtCompat.directionFromDelta(delta.x, delta.y, delta.z);
    }

    private static void stopAttackAction(ServerPlayer fake) {
        actionPack(fake).start(EntityPlayerActionPack.ActionType.ATTACK, null);
    }

    private static EntityPlayerActionPack actionPack(ServerPlayer fake) {
        return ((ServerPlayerInterface) fake).getActionPack();
    }
}
