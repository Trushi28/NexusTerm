package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * NexusTerm 'ls' command. 
 * Returns List<FileInfo> (Typed Objects).
 */
public class LsCommand implements NexusCommand {
    @Override
    public List<FileInfo> execute(CommandContext context, List<String> args, List<?> input) {
        Path root = resolvePath(context, args.isEmpty() ? "." : args.get(0));
        File[] files = root.toFile().listFiles();
        List<FileInfo> result = new ArrayList<>();

        if (!Files.exists(root)) {
            context.out().println("Path not found: " + root);
            return result;
        }

        if (files != null) {
            for (File f : files) {
                try {
                    String perms = "rwxr-xr-x"; // Default fallback
                    try {
                        perms = PosixFilePermissions.toString(Files.getPosixFilePermissions(f.toPath()));
                    } catch (UnsupportedOperationException e) {
                        // Not a POSIX system
                    }

                    result.add(new FileInfo(
                        f.getName(),
                        f.length(),
                        f.isDirectory(),
                        Instant.ofEpochMilli(f.lastModified()),
                        perms
                    ));
                } catch (Exception e) {
                    // Skip files we can't read
                }
            }
        }

        result.sort(Comparator.comparing(FileInfo::isDirectory).reversed().thenComparing(FileInfo::name));
        return result;
    }

    private Path resolvePath(CommandContext context, String pathStr) {
        Path candidate = Paths.get(pathStr);
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }
        return Paths.get(context.getCwd()).resolve(candidate).normalize();
    }
}
