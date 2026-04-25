package io.nexusterm.shell.commands;

import io.nexusterm.agent.SpyAgent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

final class SpyAgentJarBuilder {
    private static final String[] REQUIRED_PATHS = {
            "io/nexusterm/agent",
            "org/objectweb/asm",
            "org/objectweb/asm/commons"
    };

    private SpyAgentJarBuilder() {}

    static Path buildAgentJar() throws IOException {
        Path jarPath = Files.createTempFile("nexus-term-spy-agent-", ".jar");
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Agent-Class", SpyAgent.class.getName());
        attributes.putValue("Can-Redefine-Classes", "true");
        attributes.putValue("Can-Retransform-Classes", "true");

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            LinkedHashSet<String> writtenEntries = new LinkedHashSet<>();
            addResources(out, writtenEntries, SpyAgent.class, REQUIRED_PATHS[0]);
            addResources(out, writtenEntries, ClassReader.class, REQUIRED_PATHS[1]);
            addResources(out, writtenEntries, AdviceAdapter.class, REQUIRED_PATHS[2]);
        }
        return jarPath;
    }

    private static void addResources(
            JarOutputStream out,
            LinkedHashSet<String> writtenEntries,
            Class<?> anchor,
            String... resourceRoots
    ) throws IOException {
        URL location = anchor.getProtectionDomain().getCodeSource().getLocation();
        URI uri;
        try {
            uri = location.toURI();
        } catch (Exception e) {
            throw new IOException("Failed to resolve classpath URI for " + anchor.getName(), e);
        }
        Path sourcePath = Path.of(uri);

        if (Files.isDirectory(sourcePath)) {
            for (String root : resourceRoots) {
                Path rootPath = sourcePath.resolve(root);
                if (!Files.exists(rootPath)) {
                    continue;
                }
                try (Stream<Path> stream = Files.walk(rootPath)) {
                    stream.filter(Files::isRegularFile).forEach(path -> {
                        String entryName = sourcePath.relativize(path).toString().replace('\\', '/');
                        try {
                            addEntry(out, writtenEntries, entryName, Files.newInputStream(path));
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to package agent resource " + entryName, e);
                        }
                    });
                }
            }
            return;
        }

        try (FileSystem fs = newJarFileSystem(uri)) {
            for (String root : resourceRoots) {
                Path rootPath = fs.getPath("/" + root);
                if (!Files.exists(rootPath)) {
                    continue;
                }
                try (Stream<Path> stream = Files.walk(rootPath)) {
                    stream.filter(Files::isRegularFile).forEach(path -> {
                        String entryName = path.toString().substring(1).replace('\\', '/');
                        try {
                            addEntry(out, writtenEntries, entryName, Files.newInputStream(path));
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to package agent resource " + entryName, e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to read classpath resources from " + sourcePath, e);
        }
    }

    private static FileSystem newJarFileSystem(URI uri) throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put("create", "false");
        return FileSystems.newFileSystem(URI.create("jar:" + uri), env);
    }

    private static void addEntry(
            JarOutputStream out,
            LinkedHashSet<String> writtenEntries,
            String entryName,
            InputStream content
    ) throws IOException {
        try (InputStream in = content) {
            if (!writtenEntries.add(entryName)) {
                return;
            }
            JarEntry entry = new JarEntry(entryName);
            out.putNextEntry(entry);
            in.transferTo(out);
            out.closeEntry();
        }
    }
}
