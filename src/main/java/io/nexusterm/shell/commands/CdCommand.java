package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CdCommand implements NexusCommand {
    @Override
    public List<String> execute(CommandContext context, List<String> args, List<?> input) {
        Path target = args.isEmpty()
                ? Paths.get(System.getProperty("user.home"))
                : resolve(context, args.get(0));

        if (!Files.exists(target) || !Files.isDirectory(target)) {
            context.out().println("Not a directory: " + target);
            return List.of();
        }

        context.setCwd(target.toAbsolutePath().normalize().toString());
        return List.of(context.getCwd());
    }

    private Path resolve(CommandContext context, String path) {
        Path candidate = Paths.get(path);
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }
        return Paths.get(context.getCwd()).resolve(candidate).normalize();
    }
}
