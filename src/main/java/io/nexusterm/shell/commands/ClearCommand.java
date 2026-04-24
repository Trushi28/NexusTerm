package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;

import java.util.List;

public class ClearCommand implements NexusCommand {
    private static final String CLEAR_SCREEN = "\u001B[H\u001B[2J";

    @Override
    public List<?> execute(CommandContext context, List<String> args, List<?> input) {
        context.out().print(CLEAR_SCREEN);
        context.out().flush();
        return List.of();
    }
}
