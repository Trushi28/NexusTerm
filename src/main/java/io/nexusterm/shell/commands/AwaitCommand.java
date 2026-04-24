package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;
import io.nexusterm.shell.Job;

import java.util.List;

/**
 * Waits for a specific job to complete and returns its results.
 * Syntax: await @<id>
 */
public class AwaitCommand implements NexusCommand {
    @Override
    public List<?> execute(CommandContext context, List<String> args, List<?> input) {
        if (args.isEmpty()) {
            context.out().println("Usage: await @<id>");
            return null;
        }

        String handle = args.get(0);
        if (!handle.startsWith("@")) {
            context.out().println("Invalid handle: " + handle);
            return null;
        }

        int id = Integer.parseInt(handle.substring(1));
        var jobOpt = context.getJobManager().getJob(id);

        if (jobOpt.isPresent()) {
            try {
                return (List<?>) jobOpt.get().future().get();
            } catch (Exception e) {
                context.out().println("Job failed: " + e.getMessage());
            }
        } else {
            context.out().println("Job not found: " + handle);
        }
        return null;
    }
}
