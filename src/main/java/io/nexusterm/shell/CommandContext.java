package io.nexusterm.shell;

import org.jline.terminal.Terminal;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution context for NexusTerm commands.
 */
public class CommandContext {
    private final Terminal terminal;
    private final PrintStream out;
    private final JobManager jobManager;
    private final SessionManager sessionManager;
    private String cwd = System.getProperty("user.dir");
    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    public CommandContext(Terminal terminal, JobManager jobManager, SessionManager sessionManager) {
        this.terminal = terminal;
        this.out = new PrintStream(terminal.output(), true);
        this.jobManager = jobManager;
        this.sessionManager = sessionManager;
        this.cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize().toString();
    }

    public Terminal getTerminal() { return terminal; }
    public PrintStream out() { return out; }
    public JobManager getJobManager() { return jobManager; }
    public SessionManager getSessionManager() { return sessionManager; }
    public Map<String, Object> getVariables() { return variables; }

    public String getCwd() { return cwd; }
    public void setCwd(String cwd) { this.cwd = cwd; }
}
