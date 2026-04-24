package io.nexusterm.shell;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an asynchronous task in the shell.
 */
public record Job(
    int id,
    String command,
    CompletableFuture<?> future
) {
    public String getHandle() {
        return "@" + id;
    }
}
