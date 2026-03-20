package org.lucee.lucli.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ServerCommandHandlerTest {

    @TempDir
    Path tempDir;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void serverStartDryRun_portOverrideDoesNotMutateLuceeJson() throws Exception {
        Path configFile = tempDir.resolve("lucee.json");
        Files.writeString(configFile, """
            {
              "name": "test-server",
              "port": 8080,
              "dependencies": {
                "framework-one": {
                  "version": "1.0.0",
                  "mapping": "/framework-one",
                  "installPath": "dependencies/framework-one"
                }
              }
            }
            """);

        JsonNode before = MAPPER.readTree(Files.readString(configFile));

        ServerCommandHandler handler = new ServerCommandHandler(true, tempDir);
        String output = handler.executeCommand("server", new String[] {
                "start", "--dry-run", "--port", "9099"
        });

        JsonNode after = MAPPER.readTree(Files.readString(configFile));

        assertEquals(before, after, "start --dry-run with overrides must not modify lucee.json");
        assertNotNull(output);
        assertTrue(output.contains("\"port\" : 9099") || output.contains("\"port\": 9099"),
                "Dry-run output should include overridden runtime port");
        assertTrue(after.has("dependencies"), "Dependencies block must remain intact");
    }

    @Test
    void serverRunDryRun_keyValueOverrideDoesNotMutateLuceeJson() throws Exception {
        Path configFile = tempDir.resolve("lucee.json");
        Files.writeString(configFile, """
            {
              "name": "test-server",
              "port": 8080,
              "jvm": {
                "maxMemory": "512m"
              },
              "dependencies": {
                "framework-two": {
                  "version": "2.0.0",
                  "mapping": "/framework-two",
                  "installPath": "dependencies/framework-two"
                }
              }
            }
            """);

        JsonNode before = MAPPER.readTree(Files.readString(configFile));

        ServerCommandHandler handler = new ServerCommandHandler(true, tempDir);
        String output = handler.executeCommand("server", new String[] {
                "run", "--dry-run", "--port=9191", "jvm.maxMemory=768m"
        });

        JsonNode after = MAPPER.readTree(Files.readString(configFile));

        assertEquals(before, after, "run --dry-run with overrides must not modify lucee.json");
        assertNotNull(output);
        assertTrue(output.contains("\"port\" : 9191") || output.contains("\"port\": 9191"),
                "Dry-run output should include overridden runtime port");
        assertTrue(output.contains("\"maxMemory\" : \"768m\"") || output.contains("\"maxMemory\": \"768m\""),
                "Dry-run output should include one-shot key=value override");
        assertTrue(after.has("dependencies"), "Dependencies block must remain intact");
    }

    @Test
    void serverStartDryRun_includeLucee_showsDependencyMappingsFromLuceeJson() throws Exception {
        Path configFile = tempDir.resolve("lucee.json");
        Files.writeString(configFile, """
            {
              "name": "mapping-preview-test",
              "dependencies": {
                "my-framework": {
                  "type": "cfml",
                  "version": "4.3.0",
                  "source": "git",
                  "url": "https://github.com/example/my-framework.git",
                  "installPath": "vendor/my-framework",
                  "mapping": "/framework"
                }
              }
            }
            """);

        ServerCommandHandler handler = new ServerCommandHandler(true, tempDir);
        String output = handler.executeCommand("server", new String[] {
                "start", "--dry-run", "--include-lucee"
        });

        assertNotNull(output);
        String expectedPhysicalPath = tempDir.resolve("vendor")
                .resolve("my-framework")
                .toAbsolutePath()
                .normalize()
                .toString();
        assertTrue(output.contains("/framework/"),
                "Dry-run include-lucee output should contain dependency mapping key");
        assertTrue(output.contains(expectedPhysicalPath),
                "Dry-run include-lucee output should contain dependency mapping physical path");
    }
}
