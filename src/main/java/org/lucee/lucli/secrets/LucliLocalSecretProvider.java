package org.lucee.lucli.secrets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lucee.lucli.paths.LucliPaths;

import lucee.commons.io.log.Log;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.security.SecretProviderExtended;
import lucee.runtime.type.Struct;

/**
 * Lucee SecretProvider implementation backed by LuCLI's encrypted local secret store.
 *
 * <p>Custom properties:
 * <ul>
 *   <li>file: Absolute path to store file (default: ~/.lucli/secrets/local.json)</li>
 *   <li>passphraseEnv: Environment variable containing store passphrase
 *       (default: LUCLI_SECRETS_PASSPHRASE)</li>
 *   <li>passphrase: Optional inline passphrase (discouraged)</li>
 *   <li>allowPrompt: If true, provider may prompt via console when env/literal is absent
 *       (default: false)</li>
 * </ul>
 */
public class LucliLocalSecretProvider implements SecretProviderExtended {

    public static final String DEFAULT_PROVIDER_NAME = "lucli-local";
    public static final String DEFAULT_PASSPHRASE_ENV = "LUCLI_SECRETS_PASSPHRASE";

    private String name = DEFAULT_PROVIDER_NAME;
    private Path storeFile = LucliPaths.resolve().secretsStoreFile();
    private String passphraseEnv = DEFAULT_PASSPHRASE_ENV;
    private String passphraseLiteral;
    private boolean allowPrompt = false;
    private LocalSecretStore store;
    private Log log;

    @Override
    public void init(Config config, Struct properties, String name) throws PageException {
        this.name = (name == null || name.trim().isEmpty()) ? DEFAULT_PROVIDER_NAME : name.trim();
        this.storeFile = LucliPaths.resolve().secretsStoreFile();
        this.passphraseEnv = DEFAULT_PASSPHRASE_ENV;
        this.passphraseLiteral = null;
        this.allowPrompt = false;
        this.store = null;
        this.log = null;

        if (properties == null) {
            return;
        }

        Object fileValue = properties.get("file", null);
        if (fileValue != null && !toStringValue(fileValue).isBlank()) {
            this.storeFile = Paths.get(toStringValue(fileValue).trim());
        }

        Object passphraseEnvValue = properties.get("passphraseEnv", null);
        if (passphraseEnvValue != null && !toStringValue(passphraseEnvValue).isBlank()) {
            this.passphraseEnv = toStringValue(passphraseEnvValue).trim();
        }

        Object passphraseValue = properties.get("passphrase", null);
        if (passphraseValue != null && !toStringValue(passphraseValue).isBlank()) {
            this.passphraseLiteral = toStringValue(passphraseValue);
        }

        Object allowPromptValue = properties.get("allowPrompt", null);
        if (allowPromptValue != null) {
            this.allowPrompt = toBooleanValue(allowPromptValue, false);
        }
    }

    @Override
    public String getSecret(String key) throws PageException {
        LocalSecretStore local = getStore(false);
        if (local == null) {
            return null;
        }
        try {
            Optional<char[]> value = local.get(key);
            return value.map(String::new).orElse(null);
        } catch (SecretStoreException e) {
            throw providerFailure("Unable to read secret '" + key + "'", e);
        }
    }

    @Override
    public String getSecret(String key, String defaultValue) {
        try {
            String value = getSecret(key);
            return value != null ? value : defaultValue;
        } catch (PageException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getSecretAsBoolean(String key) throws PageException {
        String value = getSecret(key);
        if (value == null) {
            throw providerFailure("Secret '" + key + "' not found", null);
        }
        return toBooleanValue(value, false);
    }

    @Override
    public boolean getSecretAsBoolean(String key, boolean defaultValue) {
        String value = getSecret(key, null);
        if (value == null) {
            return defaultValue;
        }
        return toBooleanValue(value, defaultValue);
    }

    @Override
    public int getSecretAsInteger(String key) throws PageException {
        String value = getSecret(key);
        if (value == null) {
            throw providerFailure("Secret '" + key + "' not found", null);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw providerFailure("Secret '" + key + "' is not a valid integer", e);
        }
    }

    @Override
    public int getSecretAsInteger(String key, int defaultValue) {
        String value = getSecret(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean hasSecret(String key) {
        try {
            LocalSecretStore local = getStore(false);
            if (local == null) {
                return false;
            }
            return local.get(key).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void refresh() {
        this.store = null;
    }

    @Override
    public Log getLog() {
        return log;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setSecret(String key, String value) throws PageException {
        LocalSecretStore local = getStore(true);
        try {
            local.put(key, value != null ? value.toCharArray() : new char[0], null);
        } catch (SecretStoreException e) {
            throw providerFailure("Unable to set secret '" + key + "'", e);
        }
    }

    @Override
    public void setSecret(String key, boolean value) throws PageException {
        setSecret(key, String.valueOf(value));
    }

    @Override
    public void setSecret(String key, int value) throws PageException {
        setSecret(key, String.valueOf(value));
    }

    @Override
    public void removeSecret(String key) throws PageException {
        LocalSecretStore local = getStore(false);
        if (local == null) {
            return;
        }
        try {
            local.delete(key);
        } catch (SecretStoreException e) {
            throw providerFailure("Unable to remove secret '" + key + "'", e);
        }
    }

    @Override
    public List<String> listSecretNames() throws PageException {
        LocalSecretStore local = getStore(false);
        if (local == null) {
            return new ArrayList<>();
        }
        try {
            List<String> names = new ArrayList<>();
            for (SecretStore.SecretMetadata meta : local.list()) {
                names.add(meta.name());
            }
            return names;
        } catch (SecretStoreException e) {
            throw providerFailure("Unable to list secret names", e);
        }
    }

    private synchronized LocalSecretStore getStore(boolean createIfMissing) throws PageException {
        if (store != null) {
            return store;
        }

        if (!createIfMissing && !Files.exists(storeFile)) {
            return null;
        }

        char[] passphrase = resolvePassphrase();
        try {
            store = new LocalSecretStore(storeFile, passphrase);
            return store;
        } catch (SecretStoreException e) {
            throw providerFailure("Unable to initialize local secret store at " + storeFile, e);
        }
    }

    private char[] resolvePassphrase() throws PageException {
        if (passphraseLiteral != null && !passphraseLiteral.isEmpty()) {
            return passphraseLiteral.toCharArray();
        }

        if (passphraseEnv != null && !passphraseEnv.isBlank()) {
            String env = System.getenv(passphraseEnv);
            if (env != null && !env.isBlank()) {
                return env.toCharArray();
            }
        }

        if (allowPrompt && System.console() != null) {
            char[] prompted = System.console().readPassword(
                "Enter secrets passphrase for provider '%s': ",
                name
            );
            if (prompted != null && prompted.length > 0) {
                return prompted;
            }
        }

        throw providerFailure(
            "No passphrase available for provider '" + name + "'. " +
            "Set " + passphraseEnv + " or configure custom.passphrase.",
            null
        );
    }

    private static String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean toBooleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String s = toStringValue(value).trim();
        if (s.isEmpty()) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "1".equals(s)) {
            return true;
        }
        if ("false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s) || "0".equals(s)) {
            return false;
        }
        return defaultValue;
    }

    private RuntimeException providerFailure(String message, Throwable cause) {
        return (cause == null)
            ? new RuntimeException(message)
            : new RuntimeException(message, cause);
    }
}
