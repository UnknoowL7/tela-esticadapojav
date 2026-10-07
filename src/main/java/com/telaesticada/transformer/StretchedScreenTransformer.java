package com.telaesticada.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import net.minecraft.launchwrapper.IClassTransformer;

public class StretchedScreenTransformer implements IClassTransformer {

    private static final float STRETCHED_ASPECT = 4.0F / 3.0F;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {

        if (!"net.minecraft.client.renderer.EntityRenderer".equals(transformedName)) {
            return basicClass;
        }

        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, 0);

        reader.accept(new org.objectweb.asm.ClassVisitor(
                Opcodes.ASM5, writer) {

            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(
                    int access,
                    String name,
                    String desc,
                    String signature,
                    String[] exceptions) {

                org.objectweb.asm.MethodVisitor parent =
                        super.visitMethod(access, name, desc, signature, exceptions);

                return new AdviceAdapter(
                        Opcodes.ASM5,
                        parent,
                        access,
                        name,
                        desc) {

                    private final int fovLocal =
                            newLocal(Type.FLOAT_TYPE);

                    private final int aspectLocal =
                            newLocal(Type.FLOAT_TYPE);

                    private final int nearLocal =
                            newLocal(Type.FLOAT_TYPE);

                    private final int farLocal =
                            newLocal(Type.FLOAT_TYPE);

                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String descriptor,
                            boolean isInterface) {

                        if (opcode == Opcodes.INVOKESTATIC
                                && "net/minecraft/client/renderer/Project".equals(owner)
                                && "gluPerspective".equals(methodName)
                                && "(FFFF)V".equals(descriptor)) {

                            /*
                             * Antes da chamada:
                             *
                             * fov
                             * aspect
                             * near
                             * far
                             *
                             * Guardamos os quatro valores.
                             */

                            storeLocal(farLocal);
                            storeLocal(nearLocal);
                            storeLocal(aspectLocal);
                            storeLocal(fovLocal);

                            /*
                             * Recriamos a chamada usando:
                             *
                             * fov original
                             * 4:3
                             * near original
                             * far original
                             */

                            loadLocal(fovLocal);

                            push(STRETCHED_ASPECT);

                            loadLocal(nearLocal);
                            loadLocal(farLocal);

                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    owner,
                                    methodName,
                                    descriptor,
                                    isInterface
                            );

                            return;
                        }

                        super.visitMethodInsn(
                                opcode,
                                owner,
                                methodName,
                                descriptor,
                                isInterface
                        );
                    }
                };
            }
        }, 0);

        return writer.toByteArray();
    }
}
