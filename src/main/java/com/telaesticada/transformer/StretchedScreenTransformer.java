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

    private static final String MINECRAFT =
            "net/minecraft/client/Minecraft";

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

                    MethodVisitor parent = super.visitMethod(
                            access,
                            methodName,
                            descriptor,
                            signature,
                            exceptions
                    );

                    return new MethodVisitor(
                            Opcodes.ASM4,
                            parent
                    ) {

                        private int state = 0;

                        @Override
                        public void visitFieldInsn(
                                int opcode,
                                String owner,
                                String fieldName,
                                String fieldDescriptor) {

                            if (opcode == Opcodes.GETFIELD
                                    && MINECRAFT.equals(owner)) {

                                if ("displayWidth".equals(fieldName)
                                        || "field_71443_c".equals(fieldName)) {

                                    state = 1;

                                    super.visitFieldInsn(
                                            opcode,
                                            owner,
                                            fieldName,
                                            fieldDescriptor
                                    );

                                    return;
                                }

                                if (("displayHeight".equals(fieldName)
                                        || "field_71440_d".equals(fieldName))
                                        && state == 2) {

                                    state = 3;

                                    super.visitFieldInsn(
                                            opcode,
                                            owner,
                                            fieldName,
                                            fieldDescriptor
                                    );

                                    return;
                                }
                            }

                            state = 0;

                            super.visitFieldInsn(
                                    opcode,
                                    owner,
                                    fieldName,
                                    fieldDescriptor
                            );
                        }

                        @Override
                        public void visitInsn(int opcode) {

                            /*
                             * displayWidth -> float
                             */
                            if (opcode == Opcodes.I2F
                                    && state == 1) {

                                state = 2;

                                super.visitInsn(opcode);
                                return;
                            }

                            /*
                             * displayHeight -> float
                             */
                            if (opcode == Opcodes.I2F
                                    && state == 3) {

                                state = 4;

                                super.visitInsn(opcode);
                                return;
                            }

                            /*
                             * width / height
                             *
                             * Substitui o resultado por 4:3.
                             */
                            if (opcode == Opcodes.FDIV
                                    && state == 4) {

                                /*
                                 * Remove:
                                 *
                                 * width
                                 * height
                                 */
                                super.visitInsn(Opcodes.POP);
                                super.visitInsn(Opcodes.POP);

                                /*
                                 * Coloca:
                                 *
                                 * 4.0 / 3.0
                                 */
                                super.visitLdcInsn(
                                        STRETCHED_ASPECT
                                );

                                state = 0;

                                return;
                            }

                            state = 0;

                            super.visitInsn(opcode);
                        }

                        @Override
                        public void visitVarInsn(
                                int opcode,
                                int var) {

                            state = 0;

                            super.visitVarInsn(
                                    opcode,
                                    var
                            );
                        }

                        @Override
                        public void visitIntInsn(
                                int opcode,
                                int operand) {

                            state = 0;

                            super.visitIntInsn(
                                    opcode,
                                    operand
                            );
                        }

                        @Override
                        public void visitTypeInsn(
                                int opcode,
                                String type) {

                            state = 0;

                            super.visitTypeInsn(
                                    opcode,
                                    type
                            );
                        }

                        @Override
                        public void visitJumpInsn(
                                int opcode,
                                org.objectweb.asm.Label label) {

                            state = 0;

                            super.visitJumpInsn(
                                    opcode,
                                    label
                            );
                        }

                        @Override
                        public void visitLdcInsn(Object value) {

                            state = 0;

                            super.visitLdcInsn(value);
                        }

                        @Override
                        public void visitMethodInsn(
                                int opcode,
                                String owner,
                                String name,
                                String desc) {

                            state = 0;

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
             * Nunca deixa o transformer derrubar o Minecraft.
             */
            return basicClass;
        }
    }
}
