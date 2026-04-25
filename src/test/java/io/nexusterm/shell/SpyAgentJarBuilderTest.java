package io.nexusterm.shell.commands;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpyAgentJarBuilderTest {

    @Test
    void buildsLoadableAgentJar() throws Exception {
        Path jarPath = SpyAgentJarBuilder.buildAgentJar();

        assertTrue(Files.exists(jarPath));

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            assertEquals("io.nexusterm.agent.SpyAgent", jarFile.getManifest().getMainAttributes().getValue("Agent-Class"));
            assertNotNull(jarFile.getEntry("io/nexusterm/agent/SpyAgent.class"));
            assertNotNull(jarFile.getEntry("org/objectweb/asm/ClassReader.class"));
        }
    }
}
