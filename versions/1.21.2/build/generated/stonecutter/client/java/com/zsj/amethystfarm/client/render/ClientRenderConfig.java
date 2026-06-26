package com.zsj.amethystfarm.client.render;

public final class ClientRenderConfig {
    /** 单个预览最多绘制的紫水晶描边数 */
    public static int maxCrystalOutlines = 48;
    /** 描边最大渲染距离（方块） */
    public static double maxRenderDistance = 32.0;
    /** 是否绘制选区大框 */
    public static boolean drawRegionBox = true;

    private ClientRenderConfig() {}

    public static double maxRenderDistanceSqr() {
        return maxRenderDistance * maxRenderDistance;
    }
}
