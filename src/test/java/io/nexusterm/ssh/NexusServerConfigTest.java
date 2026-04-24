package io.nexusterm.ssh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NexusServerConfigTest {

    @Test
    void loadsDefaultsWhenNoOverridesAreSet() {
        String previousPort = System.getProperty("nexus.term.port");
        String previousUser = System.getProperty("nexus.term.user");
        String previousPassword = System.getProperty("nexus.term.password");

        try {
            System.clearProperty("nexus.term.port");
            System.clearProperty("nexus.term.user");
            System.clearProperty("nexus.term.password");

            NexusServerConfig config = NexusServerConfig.load();

            assertEquals(2222, config.port());
            assertEquals("admin", config.username());
            assertEquals("password", config.password());
        } finally {
            restore("nexus.term.port", previousPort);
            restore("nexus.term.user", previousUser);
            restore("nexus.term.password", previousPassword);
        }
    }

    @Test
    void systemPropertiesOverrideDefaults() {
        String previousPort = System.getProperty("nexus.term.port");
        String previousUser = System.getProperty("nexus.term.user");
        String previousPassword = System.getProperty("nexus.term.password");

        try {
            System.setProperty("nexus.term.port", "3022");
            System.setProperty("nexus.term.user", "operator");
            System.setProperty("nexus.term.password", "secret");

            NexusServerConfig config = NexusServerConfig.load();

            assertEquals(3022, config.port());
            assertEquals("operator", config.username());
            assertEquals("secret", config.password());
        } finally {
            restore("nexus.term.port", previousPort);
            restore("nexus.term.user", previousUser);
            restore("nexus.term.password", previousPassword);
        }
    }

    private static void restore(String propertyName, String value) {
        if (value == null) {
            System.clearProperty(propertyName);
        } else {
            System.setProperty(propertyName, value);
        }
    }
}
