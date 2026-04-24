package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.util.List;

/**
 * NexusTerm 'spy' command.
 * Attaches to a PID and injects the SpyAgent.
 * Syntax: spy <pid>
 */
public class SpyCommand implements NexusCommand {
    @Override
    public List<?> execute(CommandContext context, List<String> args, List<?> input) {
        if (args.isEmpty()) {
            context.out().println("Usage: spy <pid>");
            return null;
        }

        String pid = args.get(0);
        try {
            context.out().println("Attaching to PID " + pid + "...");
            VirtualMachine vm = VirtualMachine.attach(pid);
            
            // In a real environment, we'd point to the actual agent JAR.
            // For this project, we'll assume it's built as nexus-term-agent.jar
            String agentPath = new File("nexus-term-agent.jar").getAbsolutePath();
            
            if (!new File(agentPath).exists()) {
                context.out().println("Warning: Agent JAR not found at " + agentPath);
                context.out().println("Demo: Simulation of attachment successful.");
            } else {
                vm.loadAgent(agentPath);
                context.out().println("Agent injected successfully.");
            }
            
            vm.detach();
            context.out().println("Detached from " + pid);
            
        } catch (Exception e) {
            context.out().println("Failed to spy on " + pid + ": " + e.getMessage());
        }
        
        return null;
    }
}
