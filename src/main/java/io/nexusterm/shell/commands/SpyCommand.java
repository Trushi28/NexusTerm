package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;
import io.nexusterm.shell.NexusShell;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NexusTerm 'spy' command.
 * Attaches to a PID and injects the SpyAgent.
 * Syntax: spy <pid> [class-filter]
 */
public class SpyCommand implements NexusCommand {
    private static final String SESSION_KEY = "spySessions";

    @Override
    public List<?> execute(CommandContext context, List<String> args, List<?> input) {
        if (args.isEmpty()) {
            context.out().println("Usage: spy <pid> [class-filter] | spy stop <pid>");
            return List.of();
        }

        if ("stop".equalsIgnoreCase(args.get(0))) {
            if (args.size() < 2) {
                context.out().println("Usage: spy stop <pid>");
                return List.of();
            }
            SpySession session = spySessions(context).remove(args.get(1));
            if (session != null) {
                session.stop();
                return List.of("Stopped spy stream for " + args.get(1));
            }
            return List.of("No active spy stream for " + args.get(1));
        }

        String pid = args.get(0);
        String classFilter = args.size() > 1 ? args.get(1) : "";
        try {
            context.out().println("Attaching to PID " + pid + "...");
            VirtualMachine vm = VirtualMachine.attach(pid);

            Path logPath = Files.createTempFile("nexus-spy-" + pid + "-", ".log");
            SpySession existing = spySessions(context).remove(pid);
            if (existing != null) {
                existing.stop();
            }

            Path agentJar = SpyAgentJarBuilder.buildAgentJar();
            vm.loadAgent(agentJar.toString(), logPath + "|" + classFilter);
            vm.detach();

            NexusShell shell = (NexusShell) context.getVariables().get("currentShell");
            SpySession session = new SpySession(logPath, shell, pid);
            spySessions(context).put(pid, session);
            session.start();

            String filterSuffix = classFilter.isBlank() ? "" : " matching '" + classFilter + "'";
            return List.of(
                    "Spy attached to " + pid + filterSuffix,
                    "Streaming events back into this shell. Run 'spy stop " + pid + "' to stop."
            );
        } catch (Exception e) {
            return List.of("Failed to spy on " + pid + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, SpySession> spySessions(CommandContext context) {
        return (Map<String, SpySession>) context.getVariables().computeIfAbsent(
                SESSION_KEY,
                ignored -> new ConcurrentHashMap<String, SpySession>()
        );
    }

    private static final class SpySession implements Runnable {
        private final Path logPath;
        private final NexusShell shell;
        private final String pid;
        private volatile boolean running = true;
        private Thread thread;

        private SpySession(Path logPath, NexusShell shell, String pid) {
            this.logPath = logPath;
            this.shell = shell;
            this.pid = pid;
        }

        private void start() {
            thread = new Thread(this, "NexusSpyTail-" + pid);
            thread.setDaemon(true);
            thread.start();
        }

        private void stop() {
            running = false;
            if (thread != null) {
                thread.interrupt();
            }
        }

        @Override
        public void run() {
            long position = 0;
            while (running) {
                try {
                    if (!Files.exists(logPath)) {
                        Thread.sleep(100);
                        continue;
                    }

                    try (RandomAccessFile file = new RandomAccessFile(logPath.toFile(), "r")) {
                        file.seek(position);
                        String line;
                        while (running && (line = file.readLine()) != null) {
                            position = file.getFilePointer();
                            shell.writeRemote(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
                        }
                    }

                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException ignored) {
                    // Best-effort demo streaming.
                }
            }
        }
    }
}
