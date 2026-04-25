package io.nexusterm.shell;

import io.nexusterm.shell.commands.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Registry for all available NexusTerm commands.
 */
public class CommandRegistry {
    private final Map<String, NexusCommand> commands = new HashMap<>();

    public CommandRegistry() {
        register("cd", new CdCommand());
        register("clear", new ClearCommand());
        register("echo", new EchoCommand());
        register("help", new HelpCommand(this));
        register("ls", new LsCommand());
        register("where", new WhereCommand());
        register("select", new SelectCommand());
        register("jobs", new JobsCommand());
        register("await", new AwaitCommand());
        register("pwd", new PwdCommand());
        register("set", new SetCommand());
        register("sortby", new SortByCommand());
        register("spy", new SpyCommand());
        register("collab", new CollabCommand());
        register("rewind", new RewindCommand());
    }

    public void register(String name, NexusCommand command) {
        commands.put(name.toLowerCase(), command);
    }

    public Optional<NexusCommand> getCommand(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }

    public Set<String> names() {
        return new TreeSet<>(commands.keySet());
    }
}
