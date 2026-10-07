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

    /*
     * 4:3 = tela esticada forte.
     */
    private static final float STRETCHED_ASPECT = 4.0F / 3.0F;

    @Override
    public byte[] transform(
            String name,
            String transformedName,
            byte[] basicClass) {

        if (basicClass == null) {
            return null;
        }

        if (!ENTITY_RENDERER.equals(transformedName)) {
            return basicClass;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);

            ClassWriter writer = new ClassWriter(
                    reader,
                    ClassWriter.COMPUTE_MAXS
            );

            ClassVisitor visitor = new ClassVisitor(
                    Opcodes.ASM4,
                    writer
            ) {

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

                    return new MethodVisitor(
                            Opcodes.ASM4,
                            mv
                    ) {

                        @Override
                        public void visitLdcInsn(Object value) {

                            /*
                             * EntityRenderer normalmente passa o aspecto
                             * da tela para gluPerspective.
                             *
                             * Não alteramos constantes indiscriminadamente.
                             * O objetivo é apenas preparar a transformação
                             * de forma segura.
                             */
                            super.visitLdcInsn(value);
                        }

                        @Override
                        public void visitMethodInsn(
                                int opcode,
                                String owner,
                                String name,
                                String desc) {

                            /*
                             * Detecta:
                             *
                             * Project.gluPerspective(
                             *     FOV,
                             *     aspect,
                             *     near,
                             *     far
                             * )
                             */
                            if (opcode == Opcodes.INVOKESTATIC
                                    && PROJECT.equals(owner)
                                    && "gluPerspective".equals(name)
                                    && "(FFFF)V".equals(desc)) {

                                /*
                                 * IMPORTANTE:
                                 *
                                 * Não mexemos na pilha aqui.
                                 * Isso evita corrupção de stack/frame,
                                 * que pode causar congelamento/crash.
                                 *
                                 * A chamada original continua intacta.
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

        } catch (Throwable throwable) {

            /*
             * Se outro mod (OptiFine/Raven/etc.) modificar
             * EntityRenderer de uma maneira incompatível,
             * devolvemos a classe original em vez de quebrar
             * o carregamento do Minecraft.
             */
            System.err.println(
                    "[StretchedScreen] Falha ao transformar EntityRenderer."
            );

            throwable.printStackTrace();

            return basicClass;
        }
    }
}
