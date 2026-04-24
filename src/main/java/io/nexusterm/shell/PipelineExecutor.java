package io.nexusterm.shell;

import java.util.ArrayList;
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
            List<String> parts = ShellParser.splitPipeline(line);
            List<?> currentData = null;

            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) continue;

                List<String> tokens = ShellParser.tokenize(trimmed);
                if (tokens.isEmpty()) {
                    continue;
                }

                String cmdName = tokens.get(0);
                List<String> args = new ArrayList<>();
                for (int i = 1; i < tokens.size(); i++) {
                    args.add(ShellParser.interpolate(tokens.get(i), context.getVariables()));
                }

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
