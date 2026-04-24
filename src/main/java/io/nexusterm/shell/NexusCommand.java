package io.nexusterm.shell;

import java.util.List;

/**
 * Base interface for all NexusTerm commands.
 * Commands in NexusTerm return Lists of Objects (Structured Output) 
 * instead of raw strings.
 */
public interface NexusCommand {
    /**
     * Executes the command.
     * @param context The execution context.
     * @param args Command line arguments.
     * @param input Input objects from the previous command in the pipeline (optional).
     * @return A list of objects resulting from the command.
     */
    List<?> execute(CommandContext context, List<String> args, List<?> input);
}
