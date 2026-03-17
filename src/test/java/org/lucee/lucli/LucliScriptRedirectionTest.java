package org.lucee.lucli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LucliScriptRedirectionTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void resetStaticScriptFlags() {
        LuCLI.envFilePath = null;
        LuCLI.currentEnvironment = null;
    }

    @Test
    void parseScriptOutputRedirection_noneReturnsNull() {
        LuCLI.ScriptOutputRedirection redirection = LuCLI.parseScriptOutputRedirection("cat input.txt");
        assertNull(redirection);
    }

    @Test
    void parseScriptOutputRedirection_overwriteOperator() {
        LuCLI.ScriptOutputRedirection redirection = LuCLI.parseScriptOutputRedirection("cat input.txt > out.txt");
        assertNotNull(redirection);
        assertEquals("cat input.txt", redirection.commandLine);
        assertEquals("out.txt", redirection.targetPath);
        assertFalse(redirection.append);
    }

    @Test
    void parseScriptOutputRedirection_appendOperator() {
        LuCLI.ScriptOutputRedirection redirection = LuCLI.parseScriptOutputRedirection("cat input.txt >> out.txt");
        assertNotNull(redirection);
        assertEquals("cat input.txt", redirection.commandLine);
        assertEquals("out.txt", redirection.targetPath);
        assertEquals(true, redirection.append);
    }

    @Test
    void parseScriptOutputRedirection_ignoresQuotedOperator() {
        LuCLI.ScriptOutputRedirection redirection = LuCLI.parseScriptOutputRedirection("echo \"a > b\" > out.txt");
        assertNotNull(redirection);
        assertEquals("echo \"a > b\"", redirection.commandLine);
        assertEquals("out.txt", redirection.targetPath);
        assertFalse(redirection.append);
    }

    @Test
    void parseScriptOutputRedirection_missingPathThrows() {
        assertThrows(IllegalArgumentException.class, () -> LuCLI.parseScriptOutputRedirection("cat input.txt > "));
    }

    @Test
    void executeLucliScript_overwriteRedirectionWritesOutputFile() throws Exception {
        Path input = tempDir.resolve("input.txt");
        Path output = tempDir.resolve("output.txt");
        Path script = tempDir.resolve("script.lucli");

        Files.writeString(input, "alpha", StandardCharsets.UTF_8);
        Files.write(script, List.of("cat " + input + " > " + output), StandardCharsets.UTF_8);

        int exitCode = LuCLI.executeLucliScript(script.toString());
        assertEquals(0, exitCode);
        assertEquals("alpha" + System.lineSeparator(), Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    void executeLucliScript_appendRedirectionAppendsOutputFile() throws Exception {
        Path inputOne = tempDir.resolve("input-one.txt");
        Path inputTwo = tempDir.resolve("input-two.txt");
        Path output = tempDir.resolve("output.txt");
        Path script = tempDir.resolve("script.lucli");

        Files.writeString(inputOne, "alpha", StandardCharsets.UTF_8);
        Files.writeString(inputTwo, "beta", StandardCharsets.UTF_8);
        Files.write(
            script,
            List.of(
                "cat " + inputOne + " > " + output,
                "cat " + inputTwo + " >> " + output
            ),
            StandardCharsets.UTF_8
        );

        int exitCode = LuCLI.executeLucliScript(script.toString());
        assertEquals(0, exitCode);
        assertEquals(
            "alpha" + System.lineSeparator() + "beta" + System.lineSeparator(),
            Files.readString(output, StandardCharsets.UTF_8)
        );
    }
}
