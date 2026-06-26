package com.zsj.amethystfarm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zsj.amethystfarm.client.preview.ClientMiningCache;
import com.zsj.amethystfarm.client.preview.ClientPreviewCache;
import com.zsj.amethystfarm.client.render.CachedCrystalOutline;
import com.zsj.amethystfarm.client.render.ClientRenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AmethystPreviewRenderer {
    private AmethystPreviewRenderer() {}

    public static void register() {
        //? if >=1.21.10 {
        /*net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_MAIN
            .register(AmethystPreviewRenderer::renderModern);
        *///?} else if <1.21.9 {
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.LAST
            .register(AmethystPreviewRenderer::renderLegacy);
         //?}
    }

    //? if >=1.21.10 {
    /*private static void renderModern(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        if (ClientPreviewCache.isEmpty() && ClientMiningCache.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        var consumers = context.consumers();
        if (consumers == null) {
            return;
        }

        Vec3 camera = context.worldState().cameraRenderState.pos;
        double maxDistSqr = ClientRenderConfig.maxRenderDistanceSqr();
        PoseStack matrices = context.matrices();
        //? if >=1.21.11 {
        /^VertexConsumer lines = consumers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
        ^///?} else {
        VertexConsumer lines = consumers.getBuffer(net.minecraft.client.renderer.RenderType.lines());
         //?}

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        drawModernScene(matrices, lines, camera, maxDistSqr);
        matrices.popPose();
    }

    private static void drawModernScene(PoseStack matrices, VertexConsumer lines, Vec3 camera, double maxDistSqr) {
        for (var entry : ClientPreviewCache.all().entrySet()) {
            ClientPreviewCache.CrystalPreview preview = entry.getValue();

            if (ClientRenderConfig.drawRegionBox) {
                AABB world = regionBox(preview);
                if (world.getCenter().distanceToSqr(camera) <= maxDistSqr * 4) {
                    //? if >=1.21.11 {
                    /^net.minecraft.client.renderer.ShapeRenderer.renderShape(
                        matrices, lines, net.minecraft.world.phys.shapes.Shapes.create(world),
                        0, 0, 0, packColor(0.6f, 0.2f, 1.0f, 0.6f), 1.0f
                    );
                    ^///?} else if >=1.21.10 {
                    /^net.minecraft.client.renderer.ShapeRenderer.renderLineBox(
                        matrices.last(), lines, world, 0.6f, 0.2f, 1.0f, 0.6f
                    );
                    ^///?} else {
                    net.minecraft.client.renderer.ShapeRenderer.renderLineBox(
                        matrices, lines, world, 0.6f, 0.2f, 1.0f, 0.6f
                    );
                     //?}
                }
            }

            for (CachedCrystalOutline outline : selectVisible(preview.crystalList(), camera, maxDistSqr,
                ClientRenderConfig.maxCrystalOutlines)) {
                drawModernOutline(matrices, lines, outline,
                    outline.mature() ? 0.25f : 0.75f,
                    outline.mature() ? 1.0f : 0.45f,
                    outline.mature() ? 0.45f : 1.0f);
            }
        }

        for (CachedCrystalOutline outline : ClientMiningCache.all().values()) {
            if (outline.distanceSquaredTo(camera) <= maxDistSqr) {
                drawModernOutline(matrices, lines, outline, 1.0f, 0.15f, 0.15f);
            }
        }
    }

    private static void drawModernOutline(PoseStack matrices, VertexConsumer lines, CachedCrystalOutline outline,
                                          float r, float g, float b) {
        AABB box = outline.localBounds().move(outline.pos()).inflate(0.01);
        //? if >=1.21.11 {
        /^net.minecraft.client.renderer.ShapeRenderer.renderShape(
            matrices, lines, net.minecraft.world.phys.shapes.Shapes.create(box),
            0, 0, 0, packColor(r, g, b, 0.9f), 1.0f
        );
        ^///?} else if >=1.21.10 {
        /^net.minecraft.client.renderer.ShapeRenderer.renderLineBox(matrices.last(), lines, box, r, g, b, 0.9f);
        ^///?} else {
        net.minecraft.client.renderer.ShapeRenderer.renderLineBox(matrices, lines, box, r, g, b, 0.9f);
         //?}
    }
    *///?} else if <1.21.9 {
    private static void renderLegacy(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        if (ClientPreviewCache.isEmpty() && ClientMiningCache.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        var consumers = context.consumers();
        if (consumers == null) {
            return;
        }

        Vec3 camera = context.camera().getPosition();
        double maxDistSqr = ClientRenderConfig.maxRenderDistanceSqr();
        PoseStack matrices = context.matrixStack();
        VertexConsumer lines = consumers.getBuffer(net.minecraft.client.renderer.RenderType.lines());

        for (var entry : ClientPreviewCache.all().entrySet()) {
            ClientPreviewCache.CrystalPreview preview = entry.getValue();

            if (ClientRenderConfig.drawRegionBox) {
                AABB world = regionBox(preview);
                if (world.getCenter().distanceToSqr(camera) <= maxDistSqr * 4) {
                    AABB region = world.move(-camera.x, -camera.y, -camera.z);
                    //? if >=1.21.4 {
                    net.minecraft.client.renderer.ShapeRenderer.renderLineBox(
                        matrices, lines, region, 0.6f, 0.2f, 1.0f, 0.6f
                    );
                    //?}
                }
            }

            for (CachedCrystalOutline outline : selectVisible(preview.crystalList(), camera, maxDistSqr,
                ClientRenderConfig.maxCrystalOutlines)) {
                drawLegacyOutline(matrices, lines, outline, camera,
                    outline.mature() ? 0.25f : 0.75f,
                    outline.mature() ? 1.0f : 0.45f,
                    outline.mature() ? 0.45f : 1.0f);
            }
        }

        for (CachedCrystalOutline outline : ClientMiningCache.all().values()) {
            if (outline.distanceSquaredTo(camera) > maxDistSqr) {
                continue;
            }
            drawLegacyOutline(matrices, lines, outline, camera, 1.0f, 0.15f, 0.15f);
        }
    }

    private static void drawLegacyOutline(PoseStack matrices, VertexConsumer lines, CachedCrystalOutline outline,
                                          Vec3 camera, float r, float g, float b) {
        //? if >=1.21.4 {
        AABB box = outline.localBounds()
            .move(outline.pos())
            .move(-camera.x, -camera.y, -camera.z)
            .inflate(0.01);
        net.minecraft.client.renderer.ShapeRenderer.renderLineBox(matrices, lines, box, r, g, b, 0.9f);
        //?}
    }
    //?}

    private static AABB regionBox(ClientPreviewCache.CrystalPreview preview) {
        BlockPos min = new BlockPos(
            Math.min(preview.pos1().getX(), preview.pos2().getX()),
            Math.min(preview.pos1().getY(), preview.pos2().getY()),
            Math.min(preview.pos1().getZ(), preview.pos2().getZ())
        );
        BlockPos max = new BlockPos(
            Math.max(preview.pos1().getX(), preview.pos2().getX()),
            Math.max(preview.pos1().getY(), preview.pos2().getY()),
            Math.max(preview.pos1().getZ(), preview.pos2().getZ())
        );
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    private static List<CachedCrystalOutline> selectVisible(
        List<CachedCrystalOutline> crystals,
        Vec3 camera,
        double maxDistSqr,
        int limit
    ) {
        if (crystals.isEmpty()) {
            return List.of();
        }

        List<CachedCrystalOutline> inRange = new ArrayList<>(Math.min(limit, crystals.size()));
        for (CachedCrystalOutline outline : crystals) {
            if (outline.distanceSquaredTo(camera) <= maxDistSqr) {
                inRange.add(outline);
            }
        }

        if (inRange.size() <= limit) {
            return inRange;
        }

        inRange.sort(Comparator
            .comparingDouble((CachedCrystalOutline o) -> o.distanceSquaredTo(camera))
            .thenComparingInt(o -> o.pos().getX())
            .thenComparingInt(o -> o.pos().getY())
            .thenComparingInt(o -> o.pos().getZ()));
        return List.copyOf(inRange.subList(0, limit));
    }

    private static int packColor(float r, float g, float b, float a) {
        return ((int) (a * 255.0F) << 24)
            | ((int) (r * 255.0F) << 16)
            | ((int) (g * 255.0F) << 8)
            | (int) (b * 255.0F);
    }
}
