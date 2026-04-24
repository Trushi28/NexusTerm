package io.nexusterm.shell;

import io.nexusterm.shell.commands.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for all available NexusTerm commands.
 */
public class CommandRegistry {
    private final Map<String, NexusCommand> commands = new HashMap<>();

    public CommandRegistry() {
        register("ls", new LsCommand());
        register("where", new WhereCommand());
        register("jobs", new JobsCommand());
        register("await", new AwaitCommand());
        register("spy", new SpyCommand());
        register("collab", new CollabCommand());
        register("rewind", new RewindCommand());
        // More commands will be added here
    }

    public void register(String name, NexusCommand command) {
        commands.put(name.toLowerCase(), command);
    }

    public Optional<NexusCommand> getCommand(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }
}
