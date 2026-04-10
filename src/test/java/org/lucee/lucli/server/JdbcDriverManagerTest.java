package org.lucee.lucli.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for JdbcDriverManager.
 *
 * Tests detection of required JDBC drivers from CFConfig JSON and
 * file-existence checks in lib/ext. Actual HTTP downloads are not
 * tested here — installDriver is integration-tested manually.
 */
public class JdbcDriverManagerTest {

    @TempDir
    Path tempDir;

    // ── detectRequiredDrivers ───────────────────────────────────────────

    @Test
    void detectsRequiredDrivers_fromCfConfig() {
        String cfConfig = """
                {
                    "datasources": {
                        "mydb": {
                            "class": "org.sqlite.JDBC",
                            "connectionString": "jdbc:sqlite:db/wheels.sqlite"
                        },
                        "h2db": {
                            "class": "org.h2.Driver",
                            "connectionString": "jdbc:h2:mem:test"
                        }
                    }
                }
                """;

        Map<String, JdbcDriverManager.DriverInfo> required = JdbcDriverManager.detectRequiredDrivers(cfConfig);

        assertEquals(1, required.size(), "Should detect only SQLite, not H2");
        assertTrue(required.containsKey("sqlite"));
        assertEquals("org.sqlite.JDBC", required.get("sqlite").className());
    }

    @Test
    void detectsRequiredDrivers_noDatasources() {
        String cfConfig = """
                {
                    "inspectTemplate": "once",
                    "requestTimeout": "0,0,50,0"
                }
                """;

        Map<String, JdbcDriverManager.DriverInfo> required = JdbcDriverManager.detectRequiredDrivers(cfConfig);

        assertTrue(required.isEmpty(), "No datasources should yield empty map");
    }

    @Test
    void detectsRequiredDrivers_emptyJson() {
        Map<String, JdbcDriverManager.DriverInfo> required = JdbcDriverManager.detectRequiredDrivers("{}");
        assertTrue(required.isEmpty());
    }

    @Test
    void detectsRequiredDrivers_malformedJson() {
        Map<String, JdbcDriverManager.DriverInfo> required = JdbcDriverManager.detectRequiredDrivers("not json");
        assertTrue(required.isEmpty(), "Malformed JSON should return empty map, not throw");
    }

    @Test
    void detectsRequiredDrivers_multipleSqliteDatasources() {
        String cfConfig = """
                {
                    "datasources": {
                        "db1": { "class": "org.sqlite.JDBC", "connectionString": "jdbc:sqlite:a.db" },
                        "db2": { "class": "org.sqlite.JDBC", "connectionString": "jdbc:sqlite:b.db" }
                    }
                }
                """;

        Map<String, JdbcDriverManager.DriverInfo> required = JdbcDriverManager.detectRequiredDrivers(cfConfig);

        assertEquals(1, required.size(), "Duplicate driver class should yield single entry");
        assertTrue(required.containsKey("sqlite"));
    }

    // ── isDriverInstalled ───────────────────────────────────────────────

    @Test
    void isDriverInstalled_falseWhenMissing() throws IOException {
        Path libExtDir = tempDir.resolve("lib/ext");
        Files.createDirectories(libExtDir);

        assertFalse(JdbcDriverManager.isDriverInstalled(libExtDir, "sqlite"));
    }

    @Test
    void isDriverInstalled_trueWhenPresent() throws IOException {
        Path libExtDir = tempDir.resolve("lib/ext");
        Files.createDirectories(libExtDir);
        // Create a fake JAR matching the sqlite-jdbc prefix
        Files.createFile(libExtDir.resolve("sqlite-jdbc-3.49.1.0.jar"));

        assertTrue(JdbcDriverManager.isDriverInstalled(libExtDir, "sqlite"));
    }

    @Test
    void isDriverInstalled_trueForDifferentVersion() throws IOException {
        Path libExtDir = tempDir.resolve("lib/ext");
        Files.createDirectories(libExtDir);
        // Different version should still match the prefix
        Files.createFile(libExtDir.resolve("sqlite-jdbc-3.40.0.0.jar"));

        assertTrue(JdbcDriverManager.isDriverInstalled(libExtDir, "sqlite"));
    }

    @Test
    void isDriverInstalled_falseForUnknownDriver() throws IOException {
        Path libExtDir = tempDir.resolve("lib/ext");
        Files.createDirectories(libExtDir);

        assertFalse(JdbcDriverManager.isDriverInstalled(libExtDir, "nonexistent"));
    }

    @Test
    void isDriverInstalled_falseWhenDirectoryMissing() {
        Path libExtDir = tempDir.resolve("lib/ext/does-not-exist");

        assertFalse(JdbcDriverManager.isDriverInstalled(libExtDir, "sqlite"));
    }

    // ── ensureDrivers ───────────────────────────────────────────────────

    @Test
    void ensureDrivers_skipsAlreadyInstalled() throws IOException {
        Path libExtDir = tempDir.resolve("lib/ext");
        Files.createDirectories(libExtDir);
        // Pre-install the driver
        Files.createFile(libExtDir.resolve("sqlite-jdbc-3.49.1.0.jar"));

        String cfConfig = """
                {
                    "datasources": {
                        "mydb": {
                            "class": "org.sqlite.JDBC",
                            "connectionString": "jdbc:sqlite:test.db"
                        }
                    }
                }
                """;

        boolean installed = JdbcDriverManager.ensureDrivers(libExtDir, cfConfig);

        assertFalse(installed, "Should not report new installations when driver already present");
    }

    @Test
    void ensureDrivers_returnsFalseWhenNoDriversNeeded() throws IOException {
        Path libExtDir = tempDir.resolve("lib/ext");
        Files.createDirectories(libExtDir);

        String cfConfig = """
                {
                    "datasources": {
                        "h2db": { "class": "org.h2.Driver", "connectionString": "jdbc:h2:mem:test" }
                    }
                }
                """;

        boolean installed = JdbcDriverManager.ensureDrivers(libExtDir, cfConfig);

        assertFalse(installed, "H2 is bundled with Lucee, should not trigger install");
    }
}
