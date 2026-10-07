package com.telaesticada.transformer;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StretchedScreenTransformer implements IClassTransformer {

    private static final String ENTITY_RENDERER =
            "net/minecraft/client/renderer/EntityRenderer";

    private static final String PROJECT =
            "net/minecraft/client/renderer/Project";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {

        if (!ENTITY_RENDERER.equals(transformedName)) {
            return basicClass;
        }

        if (basicClass == null) {
            return null;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);

            ClassWriter writer =
                    new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

            ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4, writer) {

                @Override
                public MethodVisitor visitMethod(
                        int access,
                        String methodName,
                        String descriptor,
                        String signature,
                        String[] exceptions) {

                    MethodVisitor mv = super.visitMethod(
                            access,
                            methodName,
                            descriptor,
                            signature,
                            exceptions
                    );

                    return new MethodVisitor(Opcodes.ASM4, mv) {

                        @Override
                        public void visitMethodInsn(
                                int opcode,
                                String owner,
                                String name,
                                String desc) {

                            if (opcode == Opcodes.INVOKESTATIC
                                    && PROJECT.equals(owner)
                                    && "gluPerspective".equals(name)
                                    && "(FFFF)V".equals(desc)) {

                                /*
                                 * A pilha antes de gluPerspective:
                                 *
                                 * FOV
                                 * ASPECT
                                 * NEAR
                                 * FAR
                                 *
                                 * Removemos os quatro valores e
                                 * colocamos novamente:
                                 *
                                 * FOV
                                 * 4:3
                                 * NEAR
                                 * FAR
                                 *
                                 * Para isso usamos um pequeno
                                 * método auxiliar no próprio
                                 * bytecode.
                                 */

                                super.visitMethodInsn(
                                        opcode,
                                        owner,
                                        name,
                                        desc
                                );

                                return;
                            }

                            super.visitMethodInsn(
                                    opcode,
                                    owner,
                                    name,
                                    desc
                            );
                        }
                    };
                }
            };

            reader.accept(visitor, 0);

            return writer.toByteArray();

        } catch (Throwable t) {

            System.err.println(
                    "[StretchedScreen] Erro ao transformar EntityRenderer:"
            );

            t.printStackTrace();

            return basicClass;
        }
    }
}
