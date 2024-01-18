package codechicken.lib.compat.microblock;

import codechicken.lib.render.BlockRenderer;
import codechicken.lib.render.CCRenderPipeline;

public class MicroblockThreadSafe {
    private static final ThreadLocal<MicroblockThreadSafe> ts = ThreadLocal.withInitial(MicroblockThreadSafe::new);

    //region MaterialRenderHelper

    private int MaterialRenderHelper$pass;
    private CCRenderPipeline.PipelineBuilder MaterialRenderHelper$builder;

    public static int MaterialRenderHelper$pass() {
        return ts.get().MaterialRenderHelper$pass;
    }

    public static void MaterialRenderHelper$pass(int v) {
        ts.get().MaterialRenderHelper$pass = v;
    }

    public static CCRenderPipeline.PipelineBuilder MaterialRenderHelper$builder() {
        return ts.get().MaterialRenderHelper$builder;
    }

    public static void MaterialRenderHelper$builder(CCRenderPipeline.PipelineBuilder v) {
        ts.get().MaterialRenderHelper$builder = v;
    }

    //endregion

    //region MicroblockRender

    private BlockRenderer.BlockFace MicroblockRender$face = new BlockRenderer.BlockFace();

    public static BlockRenderer.BlockFace MicroblockRender$face() {
        return ts.get().MicroblockRender$face;
    }

    public static void MicroblockRender$face(BlockRenderer.BlockFace v) {
        ts.get().MicroblockRender$face = v;
    }
}
