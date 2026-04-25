package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;
import io.nexusterm.shell.ShellValueSupport;

import java.util.ArrayList;
import java.util.List;

public class SortByCommand implements NexusCommand {
    @Override
    public List<?> execute(CommandContext context, List<String> args, List<?> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        if (args.isEmpty()) {
            context.out().println("Usage: sortby <field> [asc|desc]");
            return input;
        }

        String fieldName = args.get(0);
        boolean descending = args.size() > 1 && "desc".equalsIgnoreCase(args.get(1));

        List<Object> result = new ArrayList<>(input);
        result.sort((left, right) -> {
            Object leftValue = ShellValueSupport.readProperty(left, fieldName);
            Object rightValue = ShellValueSupport.readProperty(right, fieldName);

            int comparison;
            if (leftValue == null && rightValue == null) {
                comparison = 0;
            } else if (leftValue == null) {
                comparison = 1;
            } else if (rightValue == null) {
                comparison = -1;
            } else {
                comparison = ShellValueSupport.compare(leftValue, rightValue);
            }
            return descending ? -comparison : comparison;
        });
        return result;
    }
}
