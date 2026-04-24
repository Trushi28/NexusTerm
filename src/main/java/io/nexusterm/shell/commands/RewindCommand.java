package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;
import io.nexusterm.shell.NexusShell;

import java.util.List;

/**
 * Rewinds the shell session to a previous state.
 */
public class RewindCommand implements NexusCommand {
    @Override
    public List<String> execute(CommandContext context, List<String> args, List<?> input) {
        if (args.isEmpty()) {
            context.out().println("Usage: rewind <steps>");
            return null;
        }

        try {
            int steps = Integer.parseInt(args.get(0));
            NexusShell shell = (NexusShell) context.getVariables().get("currentShell");
            int currentIndex = shell.getHistorySize() - 1;
            int targetIndex = currentIndex - steps;

            if (targetIndex < 0) targetIndex = 0;

            context.out().println("Rewinding " + targetIndex + " steps to state [" + targetIndex + "]...");
            shell.restoreState(targetIndex, context);
            context.out().println("State restored. Variables and CWD reverted.");
            
        } catch (NumberFormatException e) {
            context.out().println("Error: Steps must be a number.");
        }

        return null;
    }
}
