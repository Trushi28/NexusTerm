package io.nexusterm.shell;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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
    private final Set<String> collaborativePeers = ConcurrentHashMap.newKeySet();
    private final List<SessionState> stateHistory = new ArrayList<>();

    public NexusShell(SessionManager sessionManager, String sessionId) {
        this.sessionManager = sessionManager;
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
    public SessionManager getSessionManager() { return sessionManager; }

    public void addPeer(String peerId) { collaborativePeers.add(peerId); }

    public void writeRemote(String msg) {
        if (terminal != null) {
            terminal.writer().println("\r" + msg);
            terminal.writer().flush();
        }
    }

    private void broadcast(String msg) {
        for (String peerId : collaborativePeers) {
            sessionManager.getSession(peerId).ifPresent(s -> s.writeRemote("\u001B[33m[" + sessionId + "]\u001B[0m " + msg));
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
        try {
            this.terminal = TerminalBuilder.builder()
                .type(environment.getEnv().get(Environment.ENV_TERM))
                .streams(in, out)
                .build();
            
            PrintStream outStream = new PrintStream(terminal.output(), true);
            outStream.println("\u001B[35mWelcome to NexusTerm v1.0 (2025 Edition)\u001B[0m");
            outStream.println("Your Session ID: \u001B[32m" + sessionId + "\u001B[0m");
            outStream.println("Type 'help' for a list of crazy commands.");

            CommandRegistry registry = new CommandRegistry();
            JobManager jobManager = new JobManager();
            CommandContext context = new CommandContext(terminal, jobManager, sessionManager);
            context.getVariables().put("currentShell", this);
            PipelineExecutor executor = new PipelineExecutor(registry, context);

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            while (true) {
                snapshot(context);
                String line;
                try {
                    line = reader.readLine("\u001B[34mnexus\u001B[0m [" + (stateHistory.size()-1) + "]> ");
                } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
                    break;
                }

                if (line == null || "exit".equalsIgnoreCase(line.trim())) {
                    break;
                }
                
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                boolean background = trimmed.endsWith("&");
                String cmdLine = background ? trimmed.substring(0, trimmed.length() - 1).trim() : trimmed;

                var future = executor.execute(cmdLine);
                Job job = jobManager.createJob(cmdLine, future);

                if (background) {
                    outStream.println("Background job started: " + job.getHandle());
                } else {
                    try {
                        List<?> results = (List<?>) future.get();
                        if (results != null) {
                            for (Object obj : results) {
                                String lineOut = obj.toString();
                                outStream.println(lineOut);
                                broadcast(lineOut);
                            }
                        }
                    } catch (Exception e) {
                        outStream.println("\u001B[31mJob failed: " + e.getMessage() + "\u001B[0m");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Shell error", e);
        } finally {
            sessionManager.unregister(sessionId);
            callback.onExit(0);
        }
    }
}
