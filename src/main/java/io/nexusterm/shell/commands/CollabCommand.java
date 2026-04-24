package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;
import io.nexusterm.shell.NexusShell;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages collaborative shell sessions.
 */
public class CollabCommand implements NexusCommand {
    @Override
    public List<String> execute(CommandContext context, List<String> args, List<?> input) {
        if (args.isEmpty()) {
            context.out().println("Usage: collab <list|join> [sessionId]");
            return null;
        }

        String action = args.get(0).toLowerCase();
        var sessionManager = context.getSessionManager();
        var currentShell = (NexusShell) context.getVariables().get("currentShell");

        if ("list".equals(action)) {
            return sessionManager.getActiveSessions().stream()
                    .map(s -> "Session: " + s.getSessionId() + (s == currentShell ? " (you)" : ""))
                    .collect(Collectors.toList());
        } else if ("join".equals(action)) {
            if (args.size() < 2) {
                context.out().println("Error: Specify a sessionId to join.");
                return null;
            }
            String targetId = args.get(1);
            var targetSession = sessionManager.getSession(targetId);
            
            if (targetSession.isPresent()) {
                targetSession.get().addPeer(currentShell.getSessionId());
                context.out().println("Joined session: " + targetId + ". You will now see their output.");
            } else {
                context.out().println("Error: Session not found: " + targetId);
            }
        } else {
            context.out().println("Unknown action: " + action);
        }

        return null;
    }
}
