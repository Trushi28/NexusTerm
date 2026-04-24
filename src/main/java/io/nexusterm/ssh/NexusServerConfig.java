package io.nexusterm.ssh;

final class NexusServerConfig {
    private final int port;
    private final String username;
    private final String password;

    private NexusServerConfig(int port, String username, String password) {
        this.port = port;
        this.username = username;
        this.password = password;
    }

    static NexusServerConfig load() {
        return new NexusServerConfig(
                integerValue("nexus.term.port", "NEXUS_TERM_PORT", 2222),
                stringValue("nexus.term.user", "NEXUS_TERM_USER", "admin"),
                stringValue("nexus.term.password", "NEXUS_TERM_PASSWORD", "password")
        );
    }

    int port() {
        return port;
    }

    String username() {
        return username;
    }

    String password() {
        return password;
    }

    private static int integerValue(String propertyName, String envName, int defaultValue) {
        String raw = stringValue(propertyName, envName, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for " + propertyName + "/" + envName + ": " + raw, e);
        }
    }

    private static String stringValue(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return defaultValue;
    }
}
