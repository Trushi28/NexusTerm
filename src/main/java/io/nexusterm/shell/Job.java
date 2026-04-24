package io.nexusterm.shell;

import java.util.concurrent.CompletableFuture;

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

    public String status() {
        if (!future.isDone()) {
            return "RUNNING";
        }
        if (future.isCompletedExceptionally()) {
            return "FAILED";
        }
        if (future.isCancelled()) {
            return "CANCELLED";
        }
        return "DONE";
    }

    @Override
    public String toString() {
        return "%s %-9s %s".formatted(getHandle(), status(), command);
    }
}
