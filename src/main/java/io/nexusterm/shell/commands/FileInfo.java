package io.nexusterm.shell.commands;

import java.time.Instant;

/**
 * Structured representation of a file.
 */
public record FileInfo(
    String name,
    long size,
    boolean isDirectory,
    Instant lastModified,
    String permissions
) {
    @Override
    public String toString() {
        String type = isDirectory ? "d" : "-";
        return String.format("%s %10d %s %s", permissions, size, lastModified, name);
    }
}
