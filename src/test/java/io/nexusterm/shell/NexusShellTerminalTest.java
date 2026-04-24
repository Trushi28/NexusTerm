package io.nexusterm.shell;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NexusShellTerminalTest {

    @Test
    void createsExternalTerminalForRemoteSessions() throws Exception {
        Terminal terminal = NexusShell.createTerminal(
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(),
                "xterm-kitty"
        );

        assertNotNull(terminal);
        assertEquals("xterm-kitty", terminal.getType());
    }

    @Test
    void createsPortableDumbTerminalForSshSessions() throws Exception {
        Terminal terminal = NexusShell.createDumbTerminal(
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream()
        );

        assertNotNull(terminal);
        assertEquals("dumb", terminal.getType());
    }

    @Test
    void dumbTerminalSupportsLineReaderInput() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream("help\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = NexusShell.createDumbTerminal(in, out);

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        assertEquals("help", reader.readLine(""));
    }
}
