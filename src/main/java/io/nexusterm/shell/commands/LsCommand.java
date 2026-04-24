package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NexusTerm 'ls' command. 
 * Returns List<FileInfo> (Typed Objects).
 */
public class LsCommand implements NexusCommand {
    @Override
    public List<FileInfo> execute(CommandContext context, List<String> args, List<?> input) {
        String pathStr = args.isEmpty() ? "." : args.get(0);
        Path root = Paths.get(pathStr).toAbsolutePath().normalize();
        
        File[] files = root.toFile().listFiles();
        List<FileInfo> result = new ArrayList<>();
        
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
        
        return result;
    }
}
