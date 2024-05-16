package codechicken.lib.lighting;

import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.render.CCRenderState;

/**
 * Faster precomputed version of LightModel that only works for axis planar sides
 */
public class PlanarLightModel implements CCRenderState.IVertexOperation
{

    public static class ThreadState {
        public PlanarLightModel standardLightModel = LightModel.standardLightModel().reducePlanar();
        public static ThreadState threadState() {
            return threadState.get();
        }
    }
    private static final ThreadLocal<ThreadState> threadState = ThreadLocal.withInitial(ThreadState::new);

    public static PlanarLightModel standardLightModel() {
        return threadState.get().standardLightModel;
    }

    public static void standardLightModel(PlanarLightModel v) {
        threadState.get().standardLightModel = v;
    }

    public int[] colours;

    public PlanarLightModel(int[] colours) {
        this.colours = colours;
    }

    @Override
    public boolean load() {
        if(!CCRenderState.computeLighting())
            return false;

        CCRenderState.pipeline().addDependency(CCRenderState.sideAttrib());
        CCRenderState.pipeline().addDependency(CCRenderState.colourAttrib());
        return true;
    }

    @Override
    public void operate() {
        CCRenderState.setColour(ColourRGBA.multiply(CCRenderState.colour(), colours[CCRenderState.side()]));
    }

    @Override
    public int operationID() {
        return LightModel.operationIndex;
    }
}
