package io.nexusterm.shell;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

    private CommandContext newContext() throws Exception {
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                .build();
        return new CommandContext(terminal, new JobManager(), new SessionManager());
    }
}
