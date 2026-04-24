package io.nexusterm.shell.commands;

import io.nexusterm.shell.CommandContext;
import io.nexusterm.shell.NexusCommand;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * NexusTerm 'where' command.
 * Filters input objects based on field values using reflection.
 * Syntax: where <field> <operator> <value>
 */
public class WhereCommand implements NexusCommand {
    @Override
    public List<?> execute(CommandContext context, List<String> args, List<?> input) {
        if (input == null || args.size() < 3) {
            context.out().println("Usage: where <field> <operator> <value>");
            return input;
        }

        String fieldName = args.get(0);
        String operator = args.get(1);
        String targetValue = args.get(2);

        List<Object> result = new ArrayList<>();
        for (Object obj : input) {
            try {
                Field field = findField(obj.getClass(), fieldName);
                if (field == null) continue;
                
                field.setAccessible(true);
                Object val = field.get(obj);
                
                if (match(val, operator, targetValue)) {
                    result.add(obj);
                }
            } catch (Exception e) {
                // Skip if error
            }
        }
        return result;
    }

    private Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private boolean match(Object val, String operator, String target) {
        if (val == null) return false;
        
        String valStr = val.toString();
        
        return switch (operator) {
            case "=" -> valStr.equalsIgnoreCase(target);
            case "!=" -> !valStr.equalsIgnoreCase(target);
            case ">" -> compare(val, target) > 0;
            case "<" -> compare(val, target) < 0;
            case "contains" -> valStr.toLowerCase().contains(target.toLowerCase());
            default -> false;
        };
    }

    private int compare(Object val, String target) {
        if (val instanceof Number n) {
            double v1 = n.doubleValue();
            double v2 = Double.parseDouble(target);
            return Double.compare(v1, v2);
        }
        return val.toString().compareTo(target);
    }
}
