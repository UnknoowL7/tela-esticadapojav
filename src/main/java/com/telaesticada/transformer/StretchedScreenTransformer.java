package com.telaesticada.transformer;

import net.minecraft.client.renderer.Project;
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

    private static final String TRANSFORMER =
            "com/telaesticada/transformer/StretchedScreenTransformer";

    /*
     * Aspecto desejado.
     *
     * 4:3 = stretched forte em telas largas.
     */
    private static final float STRETCHED_ASPECT = 4.0F / 3.0F;

    /*
     * Este método recebe exatamente os mesmos 4 valores
     * que Project.gluPerspective recebe.
     *
     * Nós simplesmente ignoramos o aspect original
     * e usamos 4:3.
     */
    public static void stretchedPerspective(
            float fov,
            float originalAspect,
            float near,
            float far) {

        Project.gluPerspective(
                fov,
                STRETCHED_ASPECT,
                near,
                far
        );
    }

    @Override
    public byte[] transform(
            String name,
            String transformedName,
            byte[] basicClass) {

        if (basicClass == null) {
            return null;
        }

        /*
         * Só mexemos no EntityRenderer.
         */
        if (!ENTITY_RENDERER.equals(transformedName)) {
            return basicClass;
        }

        try {

            ClassReader reader =
                    new ClassReader(basicClass);

            ClassWriter writer =
                    new ClassWriter(
                            reader,
                            ClassWriter.COMPUTE_MAXS
                    );

            ClassVisitor visitor =
                    new ClassVisitor(
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

                            MethodVisitor mv =
                                    super.visitMethod(
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
                                public void visitMethodInsn(
                                        int opcode,
                                        String owner,
                                        String name,
                                        String desc) {

                                    /*
                                     * Procura:
                                     *
                                     * Project.gluPerspective(
                                     *     float,
                                     *     float,
                                     *     float,
                                     *     float
                                     * )
                                     */
                                    if (opcode == Opcodes.INVOKESTATIC
                                            && PROJECT.equals(owner)
                                            && "gluPerspective".equals(name)
                                            && "(FFFF)V".equals(desc)) {

                                        /*
                                         * Mantemos exatamente os mesmos
                                         * 4 valores na pilha.
                                         *
                                         * Apenas mudamos o destino da chamada.
                                         */
                                        super.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                TRANSFORMER,
                                                "stretchedPerspective",
                                                "(FFFF)V"
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

            System.out.println(
                    "[StretchedScreen] EntityRenderer transformado!"
            );

            return writer.toByteArray();

        } catch (Throwable throwable) {

            System.err.println(
                    "[StretchedScreen] Erro ao transformar EntityRenderer:"
            );

            throwable.printStackTrace();

            /*
             * Se algo der errado, mantém a classe original.
             */
            return basicClass;
        }
    }
}
