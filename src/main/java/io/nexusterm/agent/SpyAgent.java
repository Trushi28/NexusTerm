package io.nexusterm.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Java Agent that instruments methods to log calls.
 */
public class SpyAgent {
    private static final AtomicReference<SpySink> ACTIVE_SINK = new AtomicReference<>();

    public static void agentmain(String agentArgs, Instrumentation inst) {
        AgentConfig config = AgentConfig.parse(agentArgs);
        ACTIVE_SINK.set(new SpySink(config.logPath()));
        MethodSpyTransformer transformer = new MethodSpyTransformer(config.classFilter());
        inst.addTransformer(transformer, true);
        try {
            for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
                if (!inst.isModifiableClass(loadedClass)) {
                    continue;
                }
                String className = loadedClass.getName().replace('.', '/');
                if (!transformer.shouldInstrument(className)) {
                    continue;
                }
                try {
                    inst.retransformClasses(loadedClass);
                } catch (Throwable ignored) {
                    // Best-effort retransformation for demo instrumentation.
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        SpySink sink = ACTIVE_SINK.get();
        if (sink != null) {
            sink.emit(message);
        }
    }

    private static class MethodSpyTransformer implements ClassFileTransformer {
        private final String classFilter;

        private MethodSpyTransformer(String classFilter) {
            this.classFilter = classFilter == null ? "" : classFilter;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (!shouldInstrument(className)) return null;

            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            
            reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                        return mv;
                    }
                    return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                        @Override
                        protected void onMethodEnter() {
                            visitLdcInsn("[NEXUS-SPY] " + className.replace('/', '.') + "#" + name);
                            visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "io/nexusterm/agent/SpyAgent",
                                    "log",
                                    "(Ljava/lang/String;)V",
                                    false
                            );
                        }
                    };
                }
            }, 0);
            
            return writer.toByteArray();
        }

        private boolean shouldInstrument(String className) {
            if (className == null) {
                return false;
            }
            if (className.startsWith("java/")
                    || className.startsWith("javax/")
                    || className.startsWith("jdk/")
                    || className.startsWith("sun/")
                    || className.startsWith("com/sun/")
                    || className.startsWith("org/objectweb/asm/")
                    || className.startsWith("io/nexusterm/agent/")) {
                return false;
            }
            return classFilter.isBlank() || className.replace('/', '.').contains(classFilter);
        }
    }

    private record AgentConfig(Path logPath, String classFilter) {
        private static AgentConfig parse(String rawArgs) {
            if (rawArgs == null || rawArgs.isBlank()) {
                throw new IllegalArgumentException("Spy agent requires a log file path");
            }
            String[] parts = rawArgs.split("\\|", 2);
            Path logPath = Path.of(parts[0]);
            String classFilter = parts.length > 1 ? parts[1].trim() : "";
            return new AgentConfig(logPath, classFilter);
        }
    }

    private static final class SpySink {
        private final Path logPath;

        private SpySink(Path logPath) {
            this.logPath = Objects.requireNonNull(logPath, "logPath");
        }

        private synchronized void emit(String message) {
            try {
                Files.createDirectories(logPath.getParent());
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(
                        logPath,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                ))) {
                    writer.println(message);
                }
            } catch (IOException ignored) {
                // Avoid breaking the instrumented target because of spy IO failures.
            }
        }
    }
}
