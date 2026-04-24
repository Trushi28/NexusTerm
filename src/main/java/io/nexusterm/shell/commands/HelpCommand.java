package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.CommandRegistry;
import io.nexusterm.shell.NexusCommand;

import java.util.List;

public class HelpCommand implements NexusCommand {
    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<String> execute(CommandContext context, List<String> args, List<?> input) {
        return List.of("Commands: " + String.join(", ", registry.names()));
    }
}
