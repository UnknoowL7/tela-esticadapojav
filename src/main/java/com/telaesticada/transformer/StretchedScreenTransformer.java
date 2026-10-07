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

    /*
     * Aspecto forçado para 4:3.
     *
     * Como a tela do Pojav é bem mais larga que 4:3,
     * o Minecraft renderiza a visão 4:3 e ela ocupa
     * toda a tela, produzindo o efeito stretched.
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

        /*
         * Só transforma EntityRenderer.
         */
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

                        /*
                         * Estado usado para detectar exatamente:
                         *
                         * Minecraft.displayWidth
                         * I2F
                         * Minecraft.displayHeight
                         * I2F
                         * FDIV
                         *
                         * Ou seja:
                         *
                         * (float)displayWidth /
                         * (float)displayHeight
                         */
                        private int aspectState = 0;

                        @Override
                        public void visitFieldInsn(
                                int opcode,
                                String owner,
                                String fieldName,
                                String fieldDescriptor) {

                            if (opcode == Opcodes.GETFIELD
                                    && "net/minecraft/client/Minecraft"
                                    .equals(owner)) {

                                /*
                                 * MCP/deobfuscated
                                 */
                                if ("displayWidth".equals(fieldName)) {

                                    aspectState = 1;

                                    super.visitFieldInsn(
                                            opcode,
                                            owner,
                                            fieldName,
                                            fieldDescriptor
                                    );

                                    return;
                                }

                                /*
                                 * SRG/obfuscated Minecraft 1.8.9
                                 */
                                if ("field_71443_c".equals(fieldName)) {

                                    aspectState = 1;

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
                                        && aspectState == 2) {

                                    aspectState = 3;

                                    super.visitFieldInsn(
                                            opcode,
                                            owner,
                                            fieldName,
                                            fieldDescriptor
                                    );

                                    return;
                                }
                            }

                            aspectState = 0;

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
                             * displayWidth -> I2F
                             */
                            if (opcode == Opcodes.I2F
                                    && aspectState == 1) {

                                aspectState = 2;

                                super.visitInsn(opcode);
                                return;
                            }

                            /*
                             * displayHeight -> I2F
                             */
                            if (opcode == Opcodes.I2F
                                    && aspectState == 3) {

                                aspectState = 4;

                                super.visitInsn(opcode);
                                return;
                            }

                            /*
                             * Aqui temos:
                             *
                             * (float)displayWidth /
                             * (float)displayHeight
                             *
                             * Na pilha:
                             *
                             * width height
                             *
                             * Removemos os dois valores e colocamos:
                             *
                             * 4.0F / 3.0F
                             */
                            if (opcode == Opcodes.FDIV
                                    && aspectState == 4) {

                                super.visitInsn(Opcodes.POP);
                                super.visitInsn(Opcodes.POP);

                                super.visitLdcInsn(
                                        STRETCHED_ASPECT
                                );

                                aspectState = 0;

                                return;
                            }

                            /*
                             * Qualquer outra instrução quebra
                             * a sequência que estamos procurando.
                             */
                            aspectState = 0;

                            super.visitInsn(opcode);
                        }

                        @Override
                        public void visitVarInsn(
                                int opcode,
                                int var) {

                            aspectState = 0;

                            super.visitVarInsn(
                                    opcode,
                                    var
                            );
                        }

                        @Override
                        public void visitIntInsn(
                                int opcode,
                                int operand) {

                            aspectState = 0;

                            super.visitIntInsn(
                                    opcode,
                                    operand
                            );
                        }

                        @Override
                        public void visitTypeInsn(
                                int opcode,
                                String type) {

                            aspectState = 0;

                            super.visitTypeInsn(
                                    opcode,
                                    type
                            );
                        }

                        @Override
                        public void visitJumpInsn(
                                int opcode,
                                org.objectweb.asm.Label label) {

                            aspectState = 0;

                            super.visitJumpInsn(
                                    opcode,
                                    label
                            );
                        }

                        @Override
                        public void visitLdcInsn(Object value) {

                            aspectState = 0;

                            super.visitLdcInsn(value);
                        }

                        @Override
                        public void visitMethodInsn(
                                int opcode,
                                String owner,
                                String name,
                                String desc) {

                            aspectState = 0;

                            super.visitMethodInsn(
                                    opcode,
                                    owner,
                                    name,
                                    desc
                            );
                        }

                        @Override
                        public void visitInsnAnnotation(
                                int typeRef,
                                org.objectweb.asm.TypePath typePath,
                                String desc,
                                boolean visible) {

                            super.visitInsnAnnotation(
                                    typeRef,
                                    typePath,
                                    desc,
                                    visible
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

            /*
             * Se houver incompatibilidade com outro mod,
             * mantém o Minecraft funcionando normalmente.
             */
            System.err.println(
                    "[StretchedScreen] Erro ao transformar EntityRenderer:"
            );

            throwable.printStackTrace();

            return basicClass;
        }
    }
                                }
