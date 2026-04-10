package org.lucee.lucli.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages automatic installation of JDBC drivers required by datasources
 * defined in .CFConfig.json.
 *
 * <p>When a CFConfig datasource references a driver class that is not bundled
 * with Lucee (e.g. {@code org.sqlite.JDBC}), this manager detects the
 * requirement and downloads the JAR from Maven Central into the server's
 * {@code lib/ext/} directory.</p>
 */
public final class JdbcDriverManager {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Descriptor for a known JDBC driver. */
    public record DriverInfo(String className, String filePrefix, String downloadUrl, String fileName) {}

    /**
     * Registry of known JDBC drivers that are NOT bundled with Lucee and
     * may need to be auto-installed. Expand this map as new drivers are
     * needed.
     */
    private static final Map<String, DriverInfo> KNOWN_DRIVERS = Map.of(
            "sqlite", new DriverInfo(
                    "org.sqlite.JDBC",
                    "sqlite-jdbc",
                    "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar",
                    "sqlite-jdbc-3.49.1.0.jar"
            )
    );

    private JdbcDriverManager() {
        // Utility class — no instantiation
    }

    /**
     * Parse a CFConfig JSON string, find datasources that reference a known
     * driver class, and return the matching {@link DriverInfo} entries.
     *
     * <p>Drivers already bundled with Lucee (e.g. H2) are not in the
     * registry and will be silently ignored.</p>
     *
     * @param cfConfigJson the raw JSON content of {@code .CFConfig.json}
     * @return map of driver key to {@link DriverInfo} for each required
     *         (non-bundled) driver; empty if none are needed or the JSON is
     *         malformed
     */
    public static Map<String, DriverInfo> detectRequiredDrivers(String cfConfigJson) {
        Map<String, DriverInfo> required = new HashMap<>();

        try {
            JsonNode root = MAPPER.readTree(cfConfigJson);
            JsonNode datasources = root.path("datasources");
            if (datasources.isMissingNode() || !datasources.isObject()) {
                return required;
            }

            var it = datasources.fields();
            while (it.hasNext()) {
                var entry = it.next();
                JsonNode dsNode = entry.getValue();
                String driverClass = dsNode.path("class").asText("");

                if (!driverClass.isEmpty()) {
                    for (var known : KNOWN_DRIVERS.entrySet()) {
                        if (known.getValue().className().equals(driverClass)) {
                            required.put(known.getKey(), known.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Malformed JSON — return empty, caller can proceed without auto-install
        }

        return required;
    }

    /**
     * Check whether a JAR matching the given driver key's file prefix
     * already exists in {@code libExtDir}.
     *
     * @param libExtDir the {@code lib/ext/} directory to scan
     * @param driverKey a key from {@link #KNOWN_DRIVERS} (e.g. "sqlite")
     * @return true if a matching JAR is found
     */
    public static boolean isDriverInstalled(Path libExtDir, String driverKey) {
        DriverInfo info = KNOWN_DRIVERS.get(driverKey);
        if (info == null || !Files.isDirectory(libExtDir)) {
            return false;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(libExtDir, info.filePrefix() + "*.jar")) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Download a JDBC driver JAR from Maven Central and save it to
     * {@code libExtDir}.
     *
     * @param libExtDir the {@code lib/ext/} directory to install into
     * @param driverKey a key from {@link #KNOWN_DRIVERS}
     * @throws IOException if the download or file write fails
     */
    public static void installDriver(Path libExtDir, String driverKey) throws IOException {
        DriverInfo info = KNOWN_DRIVERS.get(driverKey);
        if (info == null) {
            throw new IllegalArgumentException("Unknown driver key: " + driverKey);
        }

        Files.createDirectories(libExtDir);
        Path targetPath = libExtDir.resolve(info.fileName());

        System.out.println("Downloading JDBC driver: " + info.fileName() + " ...");

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(info.downloadUrl()))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to download " + info.downloadUrl() + " (HTTP " + response.statusCode() + ")");
            }

            try (InputStream body = response.body()) {
                Files.copy(body, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("Installed JDBC driver: " + targetPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted for " + info.fileName(), e);
        }
    }

    /**
     * Orchestrator: detect required drivers from CFConfig, check which are
     * already installed, and download any that are missing.
     *
     * @param libExtDir    the {@code lib/ext/} directory
     * @param cfConfigJson the raw JSON content of {@code .CFConfig.json}
     * @return true if any new drivers were installed (signals that the server
     *         may need a restart to pick them up)
     */
    public static boolean ensureDrivers(Path libExtDir, String cfConfigJson) {
        Map<String, DriverInfo> required = detectRequiredDrivers(cfConfigJson);
        if (required.isEmpty()) {
            return false;
        }

        boolean anyInstalled = false;
        for (var entry : required.entrySet()) {
            String key = entry.getKey();
            if (!isDriverInstalled(libExtDir, key)) {
                try {
                    installDriver(libExtDir, key);
                    anyInstalled = true;
                } catch (IOException e) {
                    System.err.println("Warning: Failed to install JDBC driver '" + key + "': " + e.getMessage());
                }
            }
        }

        return anyInstalled;
    }
}
