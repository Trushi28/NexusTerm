package io.nexusterm.shell;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellParserTest {

    @Test
    void splitsPipelinesOutsideQuotedSections() {
        assertEquals(
                List.of("echo a|b ", " where name contains x"),
                ShellParser.splitPipeline("echo 'a|b' | where name contains x")
        );
    }

    @Test
    void tokenizesQuotedArguments() {
        assertEquals(
                List.of("set", "region", "us west"),
                ShellParser.tokenize("set region \"us west\"")
        );
    }

    @Test
    void interpolatesShellVariables() {
        assertEquals(
                "deploy-prod",
                ShellParser.interpolate("deploy-$env", Map.of("env", "prod"))
        );
    }
}
