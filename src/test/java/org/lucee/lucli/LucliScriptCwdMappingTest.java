package org.lucee.lucli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LucliScriptCwdMappingTest {

    @TempDir
    Path tempDir;

    private Map<String, String> originalScriptEnvironment;

    @BeforeEach
    void setup() {
        originalScriptEnvironment = new HashMap<>(LuCLI.scriptEnvironment);
        LuCLI.scriptEnvironment = new HashMap<>(System.getenv());
        LuCLI.currentEnvironment = null;
        LuCLI.envFilePath = null;
        LuCLI.clearRuntimeCwd();
    }

    @AfterEach
    void cleanup() {
        LuCLI.scriptEnvironment = new HashMap<>(originalScriptEnvironment);
        LuCLI.currentEnvironment = null;
        LuCLI.envFilePath = null;
        LuCLI.clearRuntimeCwd();
    }

    @Test
    void cdThenRunRelativeScriptUsesUpdatedSessionDirectory() throws Exception {
        Path subDir = tempDir.resolve("SomeFolder");
        Files.createDirectories(subDir);
        Path childScript = subDir.resolve("child.cfs");
        Path markerFile = tempDir.resolve("relative_run_marker.txt");
        Path lucliFile = tempDir.resolve("cwd_expandpath.lucli");
        String markerPath = markerFile.toAbsolutePath().toString().replace("\\", "\\\\").replace("'", "''");
        Files.writeString(
            childScript,
            "fileWrite('" + markerPath + "', 'ok-relative-run');",
            StandardCharsets.UTF_8
        );

        Files.write(
            lucliFile,
            List.of(
                "cd " + subDir.toAbsolutePath(),
                "run child.cfs"
            ),
            StandardCharsets.UTF_8
        );
        int exit = LuCLI.executeLucliScript(lucliFile.toString());
        assertEquals(0, exit);
        assertTrue(Files.exists(markerFile), "Expected run child.cfs to execute and create marker file");
        assertEquals("ok-relative-run", Files.readString(markerFile, StandardCharsets.UTF_8));
    }

    @Test
    void cdThenCfmlCanResolveLocalComponentWithoutCwdPrefix() throws Exception {
        Path subDir = tempDir.resolve("ComponentFolder");
        Files.createDirectories(subDir);
        Path componentFile = subDir.resolve("LocalComponent.cfc");
        Path lucliFile = tempDir.resolve("cwd_component_lookup.lucli");

        Files.writeString(
            componentFile,
            "component { public string function hello(){ return 'ok-from-local'; } }",
            StandardCharsets.UTF_8
        );
        Files.write(
            lucliFile,
            List.of(
                "cd " + subDir.toAbsolutePath(),
                "cfml writeOutput(new LocalComponent().hello())"
            ),
            StandardCharsets.UTF_8
        );

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            int exit = LuCLI.executeLucliScript(lucliFile.toString());
            assertEquals(0, exit);
        } finally {
            System.setOut(originalOut);
        }

        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ok-from-local"), "Expected local component invocation output after cd");
    }
}
