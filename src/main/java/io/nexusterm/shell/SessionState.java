package io.nexusterm.shell;

import java.util.Map;

/**
 * Represents a snapshot of the shell session state.
 */
public record SessionState(
    String cwd,
    Map<String, Object> variables
) {}
