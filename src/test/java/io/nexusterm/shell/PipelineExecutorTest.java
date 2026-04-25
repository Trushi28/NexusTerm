package io.nexusterm.shell;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineExecutorTest {

    @Test
    void supportsQuotedArgumentsAndInterpolation() throws Exception {
        CommandContext context = newContext();
        PipelineExecutor executor = new PipelineExecutor(new CommandRegistry(), context);

        executor.execute("set env 'prod west'").get();
        List<?> output = executor.execute("echo deploy:$env").get();

        assertEquals(List.of("deploy:prod west"), output);
    }

    @Test
    void filtersTypedPipelineDataUsingUnits(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("small.txt"), "abc");
        Files.writeString(tempDir.resolve("large.txt"), "x".repeat(2048));

        CommandContext context = newContext();
        context.setCwd(tempDir.toString());
        PipelineExecutor executor = new PipelineExecutor(new CommandRegistry(), context);

        List<?> output = executor.execute("ls | where size >= 1kb | select name size").get();

        assertEquals(1, output.size());
        Map<?, ?> row = assertInstanceOf(Map.class, output.get(0));
        assertEquals("large.txt", row.get("name"));
    }

    @Test
    void storesTypedVariablesAndSortsPipelineData(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("old.txt"), "x".repeat(32));
        Files.writeString(tempDir.resolve("new.txt"), "x".repeat(4096));
        Files.setLastModifiedTime(tempDir.resolve("old.txt"), java.nio.file.attribute.FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));
        Files.setLastModifiedTime(tempDir.resolve("new.txt"), java.nio.file.attribute.FileTime.from(Instant.parse("2026-02-01T00:00:00Z")));

        CommandContext context = newContext();
        context.setCwd(tempDir.toString());
        PipelineExecutor executor = new PipelineExecutor(new CommandRegistry(), context);

        executor.execute("set minSize 1kb").get();
        Object minSize = context.getVariables().get("minSize");
        assertTrue(minSize instanceof Long);

        List<?> output = executor.execute("ls | where size >= $minSize | sortby lastModified desc | select name size").get();

        assertEquals(1, output.size());
        Map<?, ?> row = assertInstanceOf(Map.class, output.get(0));
        assertEquals("new.txt", row.get("name"));
    }

    private CommandContext newContext() throws Exception {
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                .build();
        return new CommandContext(terminal, new JobManager(), new SessionManager());
    }
}
