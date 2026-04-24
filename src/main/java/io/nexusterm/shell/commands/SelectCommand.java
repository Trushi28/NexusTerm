package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;
import io.nexusterm.shell.ShellValueSupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SelectCommand implements NexusCommand {
    @Override
    public List<?> execute(CommandContext context, List<String> args, List<?> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        if (args.isEmpty()) {
            context.out().println("Usage: select <field> [field...]");
            return List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object value : input) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String fieldName : args) {
                row.put(fieldName, ShellValueSupport.readProperty(value, fieldName));
            }
            rows.add(row);
        }
        return rows;
    }
}
