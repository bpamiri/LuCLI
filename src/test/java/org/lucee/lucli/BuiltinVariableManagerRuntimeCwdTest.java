package org.lucee.lucli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuiltinVariableManagerRuntimeCwdTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        LuCLI.clearRuntimeCwd();
    }

    @Test
    void createBuiltinVariablesUsesRuntimeCwdForCurrentAndScriptDir() throws Exception {
        Path runtimeCwd = tempDir.resolve("runtime-cwd");
        Files.createDirectories(runtimeCwd);
        LuCLI.setRuntimeCwd(runtimeCwd);

        BuiltinVariableManager variableManager = BuiltinVariableManager.getInstance(false, false);
        Map<String, Object> vars = variableManager.createBuiltinVariables(null, new String[0]);

        String expected = runtimeCwd.toAbsolutePath().toString();
        assertEquals(expected, vars.get(BuiltinVariableManager.CURRENT_DIR));
        assertEquals(expected, vars.get(BuiltinVariableManager.SCRIPT_DIR));
    }
}
