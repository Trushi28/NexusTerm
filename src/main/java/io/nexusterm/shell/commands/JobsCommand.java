package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;
import io.nexusterm.shell.Job;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists all active and background jobs.
 */
public class JobsCommand implements NexusCommand {
    @Override
    public List<Job> execute(CommandContext context, List<String> args, List<?> input) {
        return new ArrayList<>(context.getJobManager().listJobs().values());
    }
}
