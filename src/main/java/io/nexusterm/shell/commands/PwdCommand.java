package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;

import java.util.List;

public class PwdCommand implements NexusCommand {
    @Override
    public List<String> execute(CommandContext context, List<String> args, List<?> input) {
        return List.of(context.getCwd());
    }
}
