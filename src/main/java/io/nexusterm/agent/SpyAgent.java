package io.nexusterm.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * Java Agent that instruments methods to log calls.
 */
public class SpyAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new MethodSpyTransformer(), true);
        try {
            // Re-transform loaded classes if needed
            // inst.retransformClasses(...)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class MethodSpyTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            
            // Only spy on non-system classes for demo
            if (className.startsWith("java/") || className.startsWith("sun/")) return null;

            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            
            reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                        @Override
                        protected void onMethodEnter() {
                            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                            mv.visitLdcInsn("[NEXUS-SPY] Called: " + className + "." + name);
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
                        }
                    };
                }
            }, 0);
            
            return writer.toByteArray();
        }
    }
}
