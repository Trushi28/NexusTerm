package io.nexusterm.shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Executes a pipeline of commands (e.g., cmd1 | cmd2 | cmd3).
 * Passes typed object lists between them.
 */
public class PipelineExecutor {
    private final CommandRegistry registry;
    private final CommandContext context;

    public PipelineExecutor(CommandRegistry registry, CommandContext context) {
        this.registry = registry;
        this.context = context;
    }

    public CompletableFuture<List<?>> execute(String line) {
        return CompletableFuture.supplyAsync(() -> {
            String[] parts = line.split("\\|");
            List<?> currentData = null;

            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) continue;

                String[] cmdParts = trimmed.split("\\s+");
                String cmdName = cmdParts[0];
                List<String> args = cmdParts.length > 1 
                    ? Arrays.asList(Arrays.copyOfRange(cmdParts, 1, cmdParts.length))
                    : new ArrayList<>();

                var cmdOpt = registry.getCommand(cmdName);
                if (cmdOpt.isPresent()) {
                    currentData = cmdOpt.get().execute(context, args, currentData);
                } else {
                    throw new RuntimeException("Command not found: " + cmdName);
                }
            }
            return currentData;
        });
    }
}
