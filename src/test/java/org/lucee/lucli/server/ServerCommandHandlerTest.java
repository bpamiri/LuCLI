package org.lucee.lucli.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.lucee.lucli.LuCLI;
import org.junit.jupiter.api.Disabled;

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
    void serverStartDryRun_warmupFlagAddsWarmupEnvVarAndJvmProperty() throws Exception {
        Path configFile = tempDir.resolve("lucee.json");
        Files.writeString(configFile, """
            {
              "name": "warmup-test",
              "port": 8080,
              "jvm": {
                "additionalArgs": [
                  "-Dlucee.enable.warmup=false",
                  "-Dfoo=bar"
                ]
              }
            }
            """);

        ServerCommandHandler handler = new ServerCommandHandler(true, tempDir);
        String output = handler.executeCommand("server", new String[] {
                "start", "--dry-run", "--warmup"
        });

        assertNotNull(output);
        assertTrue(output.contains("\"LUCEE_ENABLE_WARMUP\" : \"true\"")
                || output.contains("\"LUCEE_ENABLE_WARMUP\": \"true\"")
                || output.contains("\"LUCEE_ENABLE_WARMUP\":\"true\""),
                "Dry-run output should include LUCEE_ENABLE_WARMUP=true");
        assertTrue(output.contains("-Dlucee.enable.warmup=true"),
                "Dry-run output should include warmup JVM system property");
        assertFalse(output.contains("-Dlucee.enable.warmup=false"),
                "Warmup override should replace existing lucee.enable.warmup values");
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
    void serverStartDryRun_usesGlobalEnvironmentFallbackWhenNoEnvFlag() throws Exception {
        Path configFile = tempDir.resolve("lucee.json");
        Files.writeString(configFile, """
            {
              "name": "env-fallback-start-test",
              "port": 8080,
              "environments": {
                "prod": {
                  "port": 80
                }
              }
            }
            """);

        String previous = LuCLI.currentEnvironment;
        LuCLI.currentEnvironment = "prod";
        try {
            ServerCommandHandler handler = new ServerCommandHandler(true, tempDir);
            String output = handler.executeCommand("server", new String[] {
                    "start", "--dry-run"
            });

            assertNotNull(output);
            assertTrue(output.contains("with environment: prod"),
                    "Dry-run output should indicate fallback environment from LuCLI.currentEnvironment");
            assertTrue(output.contains("\"port\" : 80") || output.contains("\"port\": 80"),
                    "Dry-run output should reflect merged prod environment override");
        } finally {
            LuCLI.currentEnvironment = previous;
        }
    }

    @Test
    void serverRunDryRun_explicitEnvOverridesGlobalEnvironmentFallback() throws Exception {
        Path configFile = tempDir.resolve("lucee.json");
        Files.writeString(configFile, """
            {
              "name": "env-fallback-run-test",
              "port": 8080,
              "environments": {
                "prod": {
                  "port": 80
                },
                "dev": {
                  "port": 8181
                }
              }
            }
            """);

        String previous = LuCLI.currentEnvironment;
        LuCLI.currentEnvironment = "prod";
        try {
            ServerCommandHandler handler = new ServerCommandHandler(true, tempDir);
            String output = handler.executeCommand("server", new String[] {
                    "run", "--dry-run", "--env", "dev"
            });

            assertNotNull(output);
            assertTrue(output.contains("with environment: dev"),
                    "Explicit --env should take precedence over global fallback environment");
            assertTrue(output.contains("\"port\" : 8181") || output.contains("\"port\": 8181"),
                    "Dry-run output should reflect explicitly selected environment overrides");
        } finally {
            LuCLI.currentEnvironment = previous;
        }
    }

    @Test
    @Disabled("Temporarily disabled while stabilizing dry-run dependency mapping preview output in CI")
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
