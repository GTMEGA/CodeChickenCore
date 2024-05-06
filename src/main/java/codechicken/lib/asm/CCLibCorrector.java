package codechicken.lib.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class CCLibCorrector implements IClassTransformer {
    private static final Map<String, List<String>> replacements = new HashMap<>();
    static {
        replacements.put("codechicken/lib/lighting/LightModel", Arrays.asList("standardLightModel"));
        replacements.put("codechicken/lib/lighting/PlanarLightMatrix", Arrays.asList("instance"));
        replacements.put("codechicken/lib/lighting/PlanarLightModel", Arrays.asList("standardLightModel"));
        replacements.put("codechicken/lib/lighting/SimpleBrightnessModel", Arrays.asList("instance"));
        replacements.put("codechicken/lib/render/BlockRenderer", Arrays.asList("fullBlock"));
        replacements.put("codechicken/lib/render/CCModel", Arrays.asList("vertPattern", "uvwPattern", "normalPattern", "polyPattern"));
        replacements.put("codechicken/lib/render/CCRenderState", Arrays.asList(
                "vertexAttributes",
                "normalAttrib",
                "colourAttrib",
                "lightingAttrib",
                "sideAttrib",
                "lightCoordAttrib",
                "model",
                "firstVertexIndex",
                "lastVertexIndex",
                "vertexIndex",
                "pipeline",
                "baseColour",
                "alphaOverride",
                "useNormals",
                "computeLighting",
                "useColour",
                "lightMatrix",
                "vert",
                "hasNormal",
                "normal",
                "hasColour",
                "colour",
                "hasBrightness",
                "brightness",
                "side",
                "lc"));
        replacements.put("codechicken/lib/render/ColourMultiplier", Arrays.asList("instance"));
        replacements.put("codechicken/lib/render/RenderUtils", Arrays.asList("vectors", "uniformRenderItem"));
    }

    private static final Logger LOG = LogManager.getLogger("CCLib Corrector");

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName.startsWith("codechicken.lib"))
            return bytes;

        ClassNode cn = new ClassNode(Opcodes.ASM5);
        ClassReader reader = new ClassReader(bytes);
        reader.accept(cn, 0);
        boolean changed = false;
        changed |= genericRedirect(cn, transformedName);
        for (ScalaSingletonThreadSafePatcher patcher: SCALA_PATCHERS) {
            changed |= patcher.patch(cn);
        }
        if (changed) {
            ClassWriter writer = new ClassWriter(0);
            cn.accept(writer);
            return writer.toByteArray();
        } else {
            return bytes;
        }
    }

    private static ScalaSingletonThreadSafePatcher[] SCALA_PATCHERS = new ScalaSingletonThreadSafePatcher[] {
            new ScalaSingletonThreadSafePatcher("codechicken/microblock/MaterialRenderHelper$",
                                                "codechicken/lib/compat/microblock/MicroblockThreadSafe",
                                                "MaterialRenderHelper",
                                                "pass", "builder"),
            new ScalaSingletonThreadSafePatcher("codechicken/microblock/MicroblockRender$",
                                                "codechicken/lib/compat/microblock/MicroblockThreadSafe",
                                                "MicroblockRender",
                                                "face")
    };

    private static class ScalaSingletonThreadSafePatcher {
        private final String className;
        private final String handlerClass;
        private final String methodPrefix;
        private final String[] fields;

        private ScalaSingletonThreadSafePatcher(String className, String handlerClass, String methodPrefix, String... fields) {
            this.className = className;
            this.handlerClass = handlerClass;
            this.methodPrefix = methodPrefix;
            this.fields = fields;
        }

        public boolean patch(ClassNode cn) {
            if (!className.equals(cn.name))
                return false;
            boolean modified = false;
            for (MethodNode methodNode: cn.methods) {
                ListIterator<AbstractInsnNode> insnList = methodNode.instructions.iterator();
                while (insnList.hasNext()) {
                    AbstractInsnNode insn = insnList.next();
                    if (!(insn instanceof FieldInsnNode)) {
                        continue;
                    }
                    FieldInsnNode field = (FieldInsnNode) insn;
                    int opcode = field.getOpcode();
                    if (opcode != Opcodes.GETFIELD && opcode != Opcodes.PUTFIELD)
                        continue;

                    if (!className.equals(field.owner))
                        continue;
                    for (String fieldName: fields) {
                        if (!field.name.equals(fieldName))
                            continue;
                        insnList.remove();
                        if (opcode == Opcodes.PUTFIELD)
                            insnList.add(new InsnNode(Opcodes.SWAP));
                        insnList.add(new InsnNode(Opcodes.POP));
                        MethodInsnNode method = new MethodInsnNode(Opcodes.INVOKESTATIC, handlerClass, methodPrefix + "$" + field.name, opcode == Opcodes.GETFIELD ? "()" + field.desc : "(" + field.desc + ")V", false);
                        insnList.add(method);
                        modified = true;
                    }
                }
            }
            return modified;
        }
    }

    private boolean genericRedirect(ClassNode cn, String transformedName) {
        boolean changed = false;
        for (MethodNode methodNode: cn.methods) {
            ListIterator<AbstractInsnNode> insnList = methodNode.instructions.iterator();
            while (insnList.hasNext()) {
                AbstractInsnNode insn = insnList.next();
                if (!(insn instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode field = (FieldInsnNode) insn;
                int opcode = field.getOpcode();
                if (opcode != Opcodes.GETSTATIC && opcode != Opcodes.PUTSTATIC)
                    continue;

                if (!replacements.containsKey(field.owner))
                    continue;

                List<String> replacementsArray = replacements.get(field.owner);

                if (!replacementsArray.contains(field.name))
                    continue;

                MethodInsnNode method = new MethodInsnNode(Opcodes.INVOKESTATIC, field.owner, field.name, opcode == Opcodes.GETSTATIC ? "()" + field.desc : "(" + field.desc + ")V", false);
                LOG.info("Redirecting field access: " + cn.name + "." + methodNode.name + " -> " + (opcode == Opcodes.GETSTATIC ? "GET" : "PUT") + " " + field.name + " " + field.desc);
                insnList.set(method);
                changed = true;
            }
        }
        return changed;
    }
}
