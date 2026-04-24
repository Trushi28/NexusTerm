package io.nexusterm.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShellParser {
    private ShellParser() {}

    public static List<String> splitPipeline(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaping = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }
            if (c == '|' && !inSingle && !inDouble) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }

        if (escaping) {
            current.append('\\');
        }
        parts.add(current.toString());
        return parts;
    }

    public static List<String> tokenize(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaping = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }
            if (Character.isWhitespace(c) && !inSingle && !inDouble) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }

        if (escaping) {
            current.append('\\');
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    public static String interpolate(String token, Map<String, Object> variables) {
        StringBuilder resolved = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c != '$') {
                resolved.append(c);
                continue;
            }

            if (i + 1 < token.length() && token.charAt(i + 1) == '{') {
                int end = token.indexOf('}', i + 2);
                if (end > i) {
                    appendVariable(resolved, variables, token.substring(i + 2, end));
                    i = end;
                    continue;
                }
            }

            int end = i + 1;
            while (end < token.length()) {
                char next = token.charAt(end);
                if (!Character.isLetterOrDigit(next) && next != '_' && next != '-') {
                    break;
                }
                end++;
            }
            if (end == i + 1) {
                resolved.append(c);
                continue;
            }

            appendVariable(resolved, variables, token.substring(i + 1, end));
            i = end - 1;
        }
        return resolved.toString();
    }

    private static void appendVariable(StringBuilder resolved, Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value != null) {
            resolved.append(value);
        }
    }
}
