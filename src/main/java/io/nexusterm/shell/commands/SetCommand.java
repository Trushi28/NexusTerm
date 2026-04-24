package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;

import java.util.List;

public class SetCommand implements NexusCommand {
    @Override
    public List<String> execute(CommandContext context, List<String> args, List<?> input) {
        if (args.size() < 2) {
            context.out().println("Usage: set <name> <value>");
            return List.of();
        }

        String name = args.get(0);
        String value = String.join(" ", args.subList(1, args.size()));
        context.getVariables().put(name, value);
        return List.of(name + "=" + value);
    }
}
