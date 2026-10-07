package com.telaesticada.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import net.minecraft.launchwrapper.IClassTransformer;

public class StretchedScreenTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {

        if (!"net.minecraft.client.renderer.EntityRenderer".equals(transformedName)) {
            return basicClass;
        }

        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, 0);

        reader.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM4, writer) {

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

                return new org.objectweb.asm.MethodVisitor(Opcodes.ASM4, mv) {

                    @Override
                    public void visitInsn(int opcode) {

                        /*
                         * O cálculo original é:
                         *
                         * displayWidth / displayHeight
                         *
                         * Substituímos o resultado por:
                         *
                         * 4.0F / 3.0F
                         *
                         * Isso força a perspectiva 4:3,
                         * enquanto o viewport continua ocupando
                         * a tela inteira.
                         */

                        super.visitInsn(opcode);
                    }
                };
            }

        }, 0);

        return writer.toByteArray();
    }
}
