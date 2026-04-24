package io.nexusterm.shell;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.terminal.impl.ExternalTerminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NexusShell implements Command, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(NexusShell.class);

    private final SessionManager sessionManager;
    private final String sessionId;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private Environment environment;
    private Thread thread;
    private Terminal terminal;
    private PrintStream sessionOut;
    private boolean plainTerminalMode;
    private final Set<String> collaborativePeers = ConcurrentHashMap.newKeySet();
    private final List<SessionState> stateHistory = new ArrayList<>();
    private volatile long lastLatencyMillis;

    public NexusShell(SessionManager sessionManager, String sessionId) {
        this.sessionManager = sessionManager;
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
    public SessionManager getSessionManager() { return sessionManager; }

    public void addPeer(String peerId) { collaborativePeers.add(peerId); }

    public void writeRemote(String msg) {
        if (sessionOut != null) {
            sessionOut.println();
            sessionOut.println(msg);
            sessionOut.flush();
        }
    }

    private void broadcast(String msg) {
        for (String peerId : collaborativePeers) {
            String prefix = plainTerminalMode ? "[" + sessionId + "] " : "\u001B[33m[" + sessionId + "]\u001B[0m ";
            sessionManager.getSession(peerId).ifPresent(s -> s.writeRemote(prefix + msg));
        }
    }

    private void snapshot(CommandContext context) {
        stateHistory.add(new SessionState(context.getCwd(), new HashMap<>(context.getVariables())));
    }

    public void restoreState(int index, CommandContext context) {
        if (index >= 0 && index < stateHistory.size()) {
            SessionState state = stateHistory.get(index);
            context.setCwd(state.cwd());
            context.getVariables().clear();
            context.getVariables().putAll(state.variables());
            // Clear future history
            while (stateHistory.size() > index + 1) {
                stateHistory.remove(stateHistory.size() - 1);
            }
        }
    }
    
    public int getHistorySize() { return stateHistory.size(); }

    @Override
    public void setInputStream(InputStream in) { this.in = in; }
    @Override
    public void setOutputStream(OutputStream out) { this.out = out; }
    @Override
    public void setErrorStream(OutputStream err) { this.err = err; }
    @Override
    public void setExitCallback(ExitCallback callback) { this.callback = callback; }

    @Override
    public void start(ChannelSession channel, Environment env) throws IOException {
        this.environment = env;
        sessionManager.register(sessionId, this);
        thread = new Thread(this, "NexusShell-Thread-" + sessionId);
        thread.start();
    }

    @Override
    public void destroy(ChannelSession channel) throws Exception {
        sessionManager.unregister(sessionId);
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        int exitCode = 0;
        try {
            this.terminal = createTerminal(in, out, resolveTerminalType());
            this.sessionOut = new PrintStream(terminal.output(), true, StandardCharsets.UTF_8);
            this.plainTerminalMode = "dumb".equalsIgnoreCase(terminal.getType());
            attachTerminalResizeListener();

            printBanner();

            CommandRegistry registry = new CommandRegistry();
            JobManager jobManager = new JobManager();
            CommandContext context = new CommandContext(terminal, jobManager, sessionManager);
            context.getVariables().put("currentShell", this);
            PipelineExecutor executor = new PipelineExecutor(registry, context);
            runLoop(context, jobManager, executor);
        } catch (Exception e) {
            exitCode = 1;
            writeBootstrapError("Shell startup failed: " + e.getMessage());
            logger.error("Shell error", e);
        } finally {
            sessionManager.unregister(sessionId);
            callback.onExit(exitCode);
        }
    }

    private void runLoop(CommandContext context, JobManager jobManager, PipelineExecutor executor) throws Exception {
        if (plainTerminalMode) {
            runPlainLoop(context, jobManager, executor);
            return;
        }

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        while (true) {
            snapshot(context);
            String line;
            try {
                line = reader.readLine(buildPrompt(context, jobManager));
            } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
                break;
            }
            if (!handleLine(context, jobManager, executor, line)) {
                break;
            }
        }
    }

    private void runPlainLoop(CommandContext context, JobManager jobManager, PipelineExecutor executor) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(terminal.input(), StandardCharsets.UTF_8));

        while (true) {
            snapshot(context);
            sessionOut.print(buildPrompt(context, jobManager));
            sessionOut.flush();
            String line = reader.readLine();
            if (!handleLine(context, jobManager, executor, line)) {
                break;
            }
        }
    }

    private boolean handleLine(CommandContext context, JobManager jobManager, PipelineExecutor executor, String line) {
        if (line == null || "exit".equalsIgnoreCase(line.trim())) {
            return false;
        }

        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return true;
        }

        boolean background = trimmed.endsWith("&");
        String cmdLine = background ? trimmed.substring(0, trimmed.length() - 1).trim() : trimmed;

        long start = System.nanoTime();
        var future = executor.execute(cmdLine);

        if (background) {
            Job job = jobManager.createJob(cmdLine, future);
            sessionOut.println("Background job started: " + job.getHandle());
            return true;
        }

        try {
            List<?> results = (List<?>) future.get();
            lastLatencyMillis = (System.nanoTime() - start) / 1_000_000;
            if (results != null) {
                for (Object obj : results) {
                    String lineOut = obj.toString();
                    sessionOut.println(lineOut);
                    broadcast(lineOut);
                }
            }
        } catch (Exception e) {
            sessionOut.println(formatError("Job failed: " + e.getMessage()));
        }
        return true;
    }

    private void printBanner() {
        sessionOut.println(styleAccent("Welcome to NexusTerm v1.0 (2025 Edition)"));
        sessionOut.println("Your Session ID: " + styleSuccess(sessionId));
        sessionOut.println("Type 'help' for a list of commands.");
    }

    static Terminal createTerminal(InputStream in, OutputStream out, String terminalType) throws IOException {
        try {
            return new ExternalTerminal("nexus-term", terminalType, in, out, StandardCharsets.UTF_8);
        } catch (IllegalStateException | IOException e) {
            logger.warn("Falling back to a dumb terminal for type {}", terminalType);
            logger.debug("External terminal initialization failed", e);
            return createDumbTerminal(in, out);
        }
    }

    static Terminal createDumbTerminal(InputStream in, OutputStream out) throws IOException {
        return new DumbTerminal("nexus-term", "dumb", in, out, StandardCharsets.UTF_8);
    }

    private String resolveTerminalType() {
        if (environment == null || environment.getEnv() == null) {
            return "xterm-256color";
        }
        return environment.getEnv().getOrDefault(Environment.ENV_TERM, "xterm-256color");
    }

    private void attachTerminalResizeListener() {
        applyInitialTerminalSize();
        if (environment != null) {
            environment.addSignalListener((channel, signal) -> applyInitialTerminalSize(), Signal.WINCH);
        }
    }

    private void applyInitialTerminalSize() {
        if (terminal == null || environment == null || environment.getEnv() == null) {
            return;
        }
        int columns = parseDimension(environment.getEnv().get(Environment.ENV_COLUMNS), 160);
        int lines = parseDimension(environment.getEnv().get(Environment.ENV_LINES), 48);
        terminal.setSize(new Size(columns, lines));
    }

    private int parseDimension(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void writeBootstrapError(String message) {
        OutputStream target = err != null ? err : out;
        if (target == null) {
            return;
        }

        try {
            PrintStream stream = new PrintStream(target, true, StandardCharsets.UTF_8);
            stream.println(message);
        } catch (Exception ignored) {
            // The SSH channel is already failing, so there is nothing useful left to do.
        }
    }

    private String buildPrompt(CommandContext context, JobManager jobManager) {
        Path cwd = Paths.get(context.getCwd());
        Path leaf = cwd.getFileName();
        String label = leaf == null ? cwd.toString() : leaf.toString();
        String prefix = plainTerminalMode ? "nexus" : "\u001B[34mnexus\u001B[0m";
        return prefix + " "
                + "[" + label + " jobs:" + jobManager.runningCount()
                + " hist:" + (stateHistory.size() - 1)
                + " last:" + lastLatencyMillis + "ms] > ";
    }

    private String styleAccent(String value) {
        return plainTerminalMode ? value : "\u001B[35m" + value + "\u001B[0m";
    }

    private String styleSuccess(String value) {
        return plainTerminalMode ? value : "\u001B[32m" + value + "\u001B[0m";
    }

    private String formatError(String value) {
        return plainTerminalMode ? value : "\u001B[31m" + value + "\u001B[0m";
    }
}
