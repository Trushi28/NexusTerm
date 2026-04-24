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
        return String.format("%s%s %10d %s %s", type, permissions, size, lastModified, name);
    }
}
