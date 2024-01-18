package codechicken.lib.render;

import codechicken.lib.colour.ColourRGBA;

public class ColourMultiplier implements CCRenderState.IVertexOperation
{
    private static class ThreadState {
        private ColourMultiplier instance = new ColourMultiplier(-1);

    }
    private static final ThreadLocal<ThreadState> threadState = ThreadLocal.withInitial(ThreadState::new);

    private static ColourMultiplier instance() {
        return threadState.get().instance;
    }

    public static ColourMultiplier instance(int colour) {
        ColourMultiplier instance = instance();
        instance.colour = colour;
        return instance;
    }

    public static final int operationIndex = CCRenderState.registerOperation();
    public int colour;

    public ColourMultiplier(int colour) {
        this.colour = colour;
    }

    @Override
    public boolean load() {
        if(colour == -1) {
            CCRenderState.setColour(-1);
            return false;
        }

        CCRenderState.pipeline().addDependency(CCRenderState.colourAttrib());
        return true;
    }

    @Override
    public void operate() {
        CCRenderState.setColour(ColourRGBA.multiply(CCRenderState.colour(), colour));
    }

    @Override
    public int operationID() {
        return operationIndex;
    }
}
