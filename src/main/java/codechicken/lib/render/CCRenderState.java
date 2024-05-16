package codechicken.lib.render;

import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.lighting.LC;
import codechicken.lib.lighting.LightMatrix;
import codechicken.lib.util.Copyable;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;

import java.util.ArrayList;

/**
 * The core of the CodeChickenLib render system.
 * Rendering operations are written to avoid object allocations by reusing static variables.
 */
public class CCRenderState
{
    private static int nextOperationIndex;

    public static int registerOperation() {
        return nextOperationIndex++;
    }

    public static int operationCount() {
        return nextOperationIndex;
    }

    /**
     * Represents an operation to be run for each vertex that operates on and modifies the current state
     */
    public static interface IVertexOperation
    {
        /**
         * Load any required references and add dependencies to the pipeline based on the current model (may be null)
         * Return false if this operation is redundant in the pipeline with the given model
         */
        public boolean load();

        /**
         * Perform the operation on the current render state
         */
        public void operate();

        /**
         * Get the unique id representing this type of operation. Duplicate operation IDs within the pipeline may have unexpected results.
         * ID shoulld be obtained from CCRenderState.registerOperation() and stored in a static variable
         */
        public int operationID();
    }

    private static final int operationIndexNormalAttrib = CCRenderState.registerOperation();
    private static final int operationIndexColourAttrib = CCRenderState.registerOperation();
    private static final int operationIndexLightingAttrib = CCRenderState.registerOperation();
    private static final int operationIndexSideAttrib = CCRenderState.registerOperation();
    private static final int operationLightCoordAttrib = CCRenderState.registerOperation();
    private static class ThreadState2 {
        private ArrayList<VertexAttribute<?>> vertexAttributes = new ArrayList<>();
    }
    private static final ThreadLocal<ThreadState2> threadState2 = ThreadLocal.withInitial(ThreadState2::new);
    private static class ThreadState {

        public VertexAttribute<Vector3[]> normalAttrib = new VertexAttribute<Vector3[]>() {
            private Vector3[] normalRef;

            @Override
            public Vector3[] newArray(int length) {
                return new Vector3[length];
            }

            @Override
            public int operationID() {
                return operationIndexNormalAttrib;
            }

            @Override
            public boolean load() {
                normalRef = model.getAttributes(this);
                if(model.hasAttribute(this))
                    return normalRef != null;

                if(model.hasAttribute(sideAttrib)) {
                    pipeline.addDependency(sideAttrib);
                    return true;
                }
                throw new IllegalStateException("Normals requested but neither normal or side attrutes are provided by the model");
            }

            @Override
            public void operate() {
                if(normalRef != null)
                    setNormal(normalRef[vertexIndex]);
                else
                    setNormal(Rotation.axes[side]);
            }
        };
        public VertexAttribute<int[]> colourAttrib = new VertexAttribute<int[]>() {
            private int[] colourRef;

            @Override
            public int[] newArray(int length) {
                return new int[length];
            }

            @Override
            public int operationID() {
                return operationIndexColourAttrib;
            }

            @Override
            public boolean load() {
                colourRef = model.getAttributes(this);
                return colourRef != null || !model.hasAttribute(this);
            }

            @Override
            public void operate() {
                if(colourRef != null)
                    setColour(ColourRGBA.multiply(baseColour, colourRef[vertexIndex]));
                else
                    setColour(baseColour);
            }
        };
        public VertexAttribute<int[]> lightingAttrib = new VertexAttribute<int[]>() {
            private int[] colourRef;

            @Override
            public int[] newArray(int length) {
                return new int[length];
            }

            @Override
            public int operationID() {
                return operationIndexLightingAttrib;
            }

            @Override
            public boolean load() {
                if(!computeLighting || !useColour || !model.hasAttribute(this))
                    return false;

                colourRef = model.getAttributes(this);
                if(colourRef != null) {
                    pipeline.addDependency(colourAttrib);
                    return true;
                }
                return false;
            }

            @Override
            public void operate() {
                setColour(ColourRGBA.multiply(colour, colourRef[vertexIndex]));
            }
        };
        public VertexAttribute<int[]> sideAttrib = new VertexAttribute<int[]>() {
            private int[] sideRef;

            @Override
            public int[] newArray(int length) {
                return new int[length];
            }

            @Override
            public int operationID() {
                return operationIndexSideAttrib;
            }

            @Override
            public boolean load() {
                sideRef = model.getAttributes(this);
                if(model.hasAttribute(this))
                    return sideRef != null;

                pipeline.addDependency(normalAttrib);
                return true;
            }

            @Override
            public void operate() {
                if(sideRef != null)
                    side = sideRef[vertexIndex];
                else
                    side = CCModel.findSide(normal);
            }
        };
        public VertexAttribute<LC[]> lightCoordAttrib = new VertexAttribute<LC[]>() {
            private LC[] lcRef;
            private Vector3 vec = new Vector3();//for computation
            private Vector3 pos = new Vector3();

            @Override
            public LC[] newArray(int length) {
                return new LC[length];
            }

            @Override
            public int operationID() {
                return operationLightCoordAttrib;
            }

            @Override
            public boolean load() {
                lcRef = model.getAttributes(this);
                if(model.hasAttribute(this))
                    return lcRef != null;

                pos.set(lightMatrix.pos.x, lightMatrix.pos.y, lightMatrix.pos.z);
                pipeline.addDependency(sideAttrib);
                pipeline.addRequirement(Transformation.operationIndex);
                return true;
            }

            @Override
            public void operate() {
                if(lcRef != null)
                    lc.set(lcRef[vertexIndex]);
                else
                    lc.compute(vec.set(vert.vec).sub(pos), side);
            }
        };

        public IVertexSource model;
        public int firstVertexIndex;
        public int lastVertexIndex;
        public int vertexIndex;
        public CCRenderPipeline pipeline = new CCRenderPipeline();

        public int baseColour;
        public int alphaOverride;
        public boolean useNormals;
        public boolean computeLighting;
        public boolean useColour;
        public LightMatrix lightMatrix = new LightMatrix();

        public Vertex5 vert = new Vertex5();
        public boolean hasNormal;
        public Vector3 normal = new Vector3();
        public boolean hasColour;
        public int colour;
        public boolean hasBrightness;
        public int brightness;

        public int side;
        public LC lc = new LC();
    }
    private static final ThreadLocal<ThreadState> threadState = ThreadLocal.withInitial(ThreadState::new);

    private static ArrayList<VertexAttribute<?>> vertexAttributes() {
        return threadState2.get().vertexAttributes;
    }

    private static int registerVertexAttribute(VertexAttribute<?> attr) {
        vertexAttributes().add(attr);
        return vertexAttributes().size()-1;
    }

    public static VertexAttribute<?> getAttribute(int index) {
        return vertexAttributes().get(index);
    }

    /**
     * Management class for a vertex attrute such as colour, normal etc
     * This class should handle the loading of the attrute from an array provided by IVertexSource.getAttributes or the computation of this attrute from others
     * @param <T> The array type for this attrute eg. int[], Vector3[]
     */
    public static abstract class VertexAttribute<T> implements IVertexOperation
    {
        public final int attributeIndex = registerVertexAttribute(this);
        /**
         * Set to true when the attrute is part of the pipeline. Should only be managed by CCRenderState when constructing the pipeline
         */
        public boolean active = false;

        /**
         * Construct a new array for storage of vertex attrutes in a model
         */
        public abstract T newArray(int length);

        @Override
        public abstract int operationID();
    }

    public static void arrayCopy(Object src, int srcPos, Object dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
        if(dst instanceof Copyable[]) {
            Object[] oa = (Object[])dst;
            Copyable<Object>[] c = (Copyable[])dst;
            for(int i = destPos; i < destPos+length; i++)
                if(c[i] != null)
                    oa[i] = c[i].copy();
        }
    }

    public static <T> T copyOf(VertexAttribute<T> attr, T src, int length) {
        T dst = attr.newArray(length);
        arrayCopy(src, 0, dst, 0, ((Object[])src).length);
        return dst;
    }

    public static interface IVertexSource
    {
        public Vertex5[] getVertices();

        /**
         * Gets an array of vertex attrutes
         * @param attr The vertex attrute to get
         * @param <T> The attrute array type
         * @return An array, or null if not computed
         */
        public <T> T getAttributes(VertexAttribute<T> attr);

        /**
         * @return True if the specified attrute is provided by this model, either by returning an array from getAttributes or by setting the state in prepareVertex
         */
        public boolean hasAttribute(VertexAttribute<?> attr);

        /**
         * Callback to set CCRenderState for a vertex before the pipeline runs
         */
        public void prepareVertex();
    }

    public static VertexAttribute<Vector3[]> normalAttrib() {
        return threadState.get().normalAttrib;
    }
    public static VertexAttribute<int[]> colourAttrib() {
        return threadState.get().colourAttrib;
    }
    public static VertexAttribute<int[]> lightingAttrib() {
        return threadState.get().lightingAttrib;
    }
    public static VertexAttribute<int[]> sideAttrib() {
        return threadState.get().sideAttrib;
    }

    /**
     * Uses the position of the lightmatrix to compute LC if not provided
     */
    public static VertexAttribute<LC[]> lightCoordAttrib() {
        return threadState.get().lightCoordAttrib;
    }

    //pipeline state
    public static IVertexSource model() {
        return threadState.get().model;
    }

    public static void model(IVertexSource v) {
        threadState.get().model = v;
    }

    public static int firstVertexIndex() {
        return threadState.get().firstVertexIndex;
    }

    public static void firstVertexIndex(int v) {
        threadState.get().firstVertexIndex = v;
    }

    public static int lastVertexIndex() {
        return threadState.get().lastVertexIndex;
    }

    public static void lastVertexIndex(int v) {
        threadState.get().lastVertexIndex = v;
    }

    public static int vertexIndex() {
        return threadState.get().vertexIndex;
    }

    public static void vertexIndex(int v) {
        threadState.get().vertexIndex = v;
    }

    public static CCRenderPipeline pipeline() {
        return threadState.get().pipeline;
    }

    public static void pipeline(CCRenderPipeline v) {
        threadState.get().pipeline = v;
    }

    //context

    public static int baseColour() {
        return threadState.get().baseColour;
    }

    public static void baseColour(int v) {
        threadState.get().baseColour = v;
    }

    public static int alphaOverride() {
        return threadState.get().alphaOverride;
    }

    public static void alphaOverride(int v) {
        threadState.get().alphaOverride = v;
    }

    public static boolean useNormals() {
        return threadState.get().useNormals;
    }

    public static void useNormals(boolean v) {
        threadState.get().useNormals = v;
    }

    public static boolean computeLighting() {
        return threadState.get().computeLighting;
    }

    public static void computeLighting(boolean v) {
        threadState.get().computeLighting = v;
    }

    public static boolean useColour() {
        return threadState.get().useColour;
    }

    public static void useColour(boolean v) {
        threadState.get().useColour = v;
    }

    public static LightMatrix lightMatrix() {
        return threadState.get().lightMatrix;
    }

    public static void lightMatrix(LightMatrix v) {
        threadState.get().lightMatrix = v;
    }

    //vertex outputs

    public static Vertex5 vert() {
        return threadState.get().vert;
    }

    public static void vert(Vertex5 v) {
        threadState.get().vert = v;
    }

    public static boolean hasNormal() {
        return threadState.get().hasNormal;
    }

    public static void hasNormal(boolean v) {
        threadState.get().hasNormal = v;
    }

    public static Vector3 normal() {
        return threadState.get().normal;
    }

    public static void normal(Vector3 v) {
        threadState.get().normal = v;
    }

    public static boolean hasColour() {
        return threadState.get().hasColour;
    }

    public static void hasColour(boolean v) {
        threadState.get().hasColour = v;
    }

    public static int colour() {
        return threadState.get().colour;
    }

    public static void colour(int v) {
        threadState.get().colour = v;
    }

    public static boolean hasBrightness() {
        return threadState.get().hasBrightness;
    }

    public static void hasBrightness(boolean v) {
        threadState.get().hasBrightness = v;
    }

    public static int brightness() {
        return threadState.get().brightness;
    }

    public static void brightness(int v) {
        threadState.get().brightness = v;
    }

    //attrute storage
    public static int side() {
        return threadState.get().side;
    }

    public static void side(int v) {
        threadState.get().side = v;
    }

    public static LC lc() {
        return threadState.get().lc;
    }

    public static void lc(LC v) {
        threadState.get().lc = v;
    }

    public static void reset() {
        model(null);
        pipeline().reset();
        useNormals(false);
        hasNormal(false);
        hasBrightness(false);
        hasColour(false);
        useColour(true);
        computeLighting(true);
        baseColour(-1);
        alphaOverride(-1);
    }

    public static void setPipeline(IVertexOperation... ops) {
        pipeline().setPipeline(ops);
    }

    public static void setPipeline(IVertexSource model, int start, int end, IVertexOperation... ops) {
        pipeline().reset();
        setModel(model, start, end);
        pipeline().setPipeline(ops);
    }

    public static void bindModel(IVertexSource model) {
        if(CCRenderState.model() != model) {
            CCRenderState.model(model);
            pipeline().rebuild();
        }
    }

    public static void setModel(IVertexSource source) {
        setModel(source, 0, source.getVertices().length);
    }

    public static void setModel(IVertexSource source, int start, int end) {
        bindModel(source);
        setVertexRange(start, end);
    }

    public static void setVertexRange(int start, int end) {
        firstVertexIndex(start);
        lastVertexIndex(end);
    }

    public static void render(IVertexOperation... ops) {
        setPipeline(ops);
        render();
    }

    public static void render() {
        Vertex5[] verts = model().getVertices();
        for(vertexIndex(firstVertexIndex()); vertexIndex() < lastVertexIndex(); vertexIndex(vertexIndex() + 1)) {
            model().prepareVertex();
            vert().set(verts[vertexIndex()]);
            runPipeline();
            writeVert();
        }
    }

    public static void runPipeline() {
        pipeline().operate();
    }

    public static void writeVert() {
        if(hasNormal())
            Tessellator.instance.setNormal((float)normal().x, (float)normal().y, (float)normal().z);
        if(hasColour())
            Tessellator.instance.setColorRGBA(colour()>>>24, colour()>>16 & 0xFF, colour()>>8 & 0xFF, alphaOverride() >= 0 ? alphaOverride() : colour() & 0xFF);
        if(hasBrightness())
            Tessellator.instance.setBrightness(brightness());
        Tessellator.instance.addVertexWithUV(vert().vec.x, vert().vec.y, vert().vec.z, vert().uv.u, vert().uv.v);
    }

    public static void setNormal(double x, double y, double z) {
        hasNormal(true);
        normal().set(x, y, z);
    }

    public static void setNormal(Vector3 n) {
        hasNormal(true);
        normal().set(n);
    }

    public static void setColour(int c) {
        hasColour(true);
        colour(c);
    }

    public static void setBrightness(int b) {
        hasBrightness(true);
        brightness(b);
    }

    public static void setBrightness(IBlockAccess world, int x, int y, int z) {
        setBrightness(world.getBlock(x, y, z).getMixedBrightnessForBlock(world, x, y, z));
    }

    public static void pullLightmap() {
        setBrightness((int)OpenGlHelper.lastBrightnessY << 16 | (int)OpenGlHelper.lastBrightnessX);
    }

    public static void pushLightmap() {
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness() & 0xFFFF, brightness() >>> 16);
    }

    /**
     * Compact helper for setting dynamic rendering context. Uses normals and doesn't compute lighting
     */
    public static void setDynamic() {
        useNormals(true);
        computeLighting(false);
    }

    public static void changeTexture(String texture) {
        changeTexture(new ResourceLocation(texture));
    }

    public static void changeTexture(ResourceLocation texture) {
        Minecraft.getMinecraft().renderEngine.bindTexture(texture);
    }

    public static void startDrawing() {
        startDrawing(7);
    }

    public static void startDrawing(int mode) {
        Tessellator.instance.startDrawing(mode);
        if(hasColour())
            Tessellator.instance.setColorRGBA(colour()>>>24, colour()>>16 & 0xFF, colour()>>8 & 0xFF, alphaOverride() >= 0 ? alphaOverride() : colour() & 0xFF);
        if(hasBrightness())
            Tessellator.instance.setBrightness(brightness());
    }

    public static void draw() {
        Tessellator.instance.draw();
    }
}
