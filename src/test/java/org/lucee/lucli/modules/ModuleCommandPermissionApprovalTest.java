package org.lucee.lucli.modules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ModuleCommandPermissionApprovalTest {

    @TempDir
    Path tempDir;

    private String originalLucliHome;

    @BeforeEach
    void setup() {
        originalLucliHome = System.getProperty("lucli.home");
        System.setProperty("lucli.home", tempDir.resolve("lucli-home").toString());
    }

    @AfterEach
    void cleanup() {
        if (originalLucliHome == null) {
            System.clearProperty("lucli.home");
        } else {
            System.setProperty("lucli.home", originalLucliHome);
        }
    }

    @Test
    void nonInteractiveWithoutForceFailsWithHelpfulMessage() throws Exception {
        if (System.console() != null) {
            return;
        }

        ModuleConfig config = writeAndLoadModuleWithPermissions(tempDir.resolve("module-no-force"));
        Method ensureMethod = getEnsurePermissionsMethod();

        InvocationTargetException thrown = assertThrows(
            InvocationTargetException.class,
            () -> ensureMethod.invoke(null, "bitbucket", config, false)
        );

        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof IOException);
        assertTrue(cause.getMessage().contains("Re-run with --force"));
        assertFalse(Files.exists(tempDir.resolve("lucli-home").resolve("settings.json")));
    }

    @Test
    void forceBypassesPromptAndPersistsPermissionGrant() throws Exception {
        ModuleConfig config = writeAndLoadModuleWithPermissions(tempDir.resolve("module-force"));
        Method ensureMethod = getEnsurePermissionsMethod();

        ensureMethod.invoke(null, "bitbucket", config, true);

        Path settingsPath = tempDir.resolve("lucli-home").resolve("settings.json");
        assertTrue(Files.exists(settingsPath));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(Files.readString(settingsPath));
        JsonNode grant = root.path("modulePermissions").path("bitbucket");

        assertTrue(grant.isObject());
        assertTrue(arrayContains(grant.path("env"), "BITBUCKET_WORKSPACE"));
        assertTrue(arrayContains(grant.path("env"), "BITBUCKET_REPO_SLUG"));
        assertTrue(arrayContains(grant.path("env"), "BITBUCKET_AUTH_USER"));
        assertTrue(arrayContains(grant.path("secrets"), "BITBUCKET_AUTH_TOKEN"));
        assertTrue(grant.hasNonNull("grantedAt"));
    }

    private Method getEnsurePermissionsMethod() throws Exception {
        Method method = ModuleCommand.class.getDeclaredMethod(
            "ensureModulePermissionsGranted",
            String.class,
            ModuleConfig.class,
            boolean.class
        );
        method.setAccessible(true);
        return method;
    }

    private ModuleConfig writeAndLoadModuleWithPermissions(Path moduleDir) throws Exception {
        Files.createDirectories(moduleDir);
        Files.writeString(moduleDir.resolve("module.json"), """
            {
              "name": "bitbucket",
              "version": "1.0.0",
              "permissions": {
                "env": [
                  { "alias": "BITBUCKET_WORKSPACE", "required": true },
                  { "alias": "BITBUCKET_REPO_SLUG", "required": true },
                  { "alias": "BITBUCKET_AUTH_USER", "required": true }
                ],
                "secrets": [
                  { "alias": "BITBUCKET_AUTH_TOKEN", "required": true }
                ]
              }
            }
            """);
        return ModuleConfig.load(moduleDir);
    }

    private boolean arrayContains(JsonNode node, String value) {
        if (node == null || !node.isArray()) {
            return false;
        }
        for (JsonNode element : node) {
            if (element.isTextual() && value.equals(element.asText())) {
                return true;
            }
        }
        return false;
    }
}
