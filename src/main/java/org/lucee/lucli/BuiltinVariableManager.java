package org.lucee.lucli;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

/**
 * Centralized manager for LuCLI built-in variables that should be available to all scripts.
 * 
 * This class ensures consistency across all execution paths (CLI, interactive, modules, etc.)
 * by providing a single place to define and set up all the __X variables that scripts can use.
 */
public class BuiltinVariableManager {
    
    private static BuiltinVariableManager instance;
    
    // Built-in variable names - centralized definition
    public static final String SCRIPT_FILE = "__scriptFile";
    public static final String SCRIPT_PATH = "__scriptPath";  
    public static final String SCRIPT_DIR = "__scriptDir";
    public static final String ARGUMENTS = "__arguments";
    public static final String ARGUMENT_COUNT = "__argumentCount";
    public static final String LEGACY_ARGS = "ARGS";
    public static final String LEGACY_ARGV = "ARGV";
    public static final String NAMED_ARGUMENTS = "__namedArguments";
    public static final String ENV = "__env";
    public static final String SYSTEM_PROPS = "__systemProps";
    public static final String LUCLI_HOME = "__lucliHome";
    public static final String LUCLI_VERSION = "__lucliVersion";
    public static final String CURRENT_DIR = "__currentDir";
    
    private final boolean verbose;
    private final boolean debug;
    
    private BuiltinVariableManager(boolean verbose, boolean debug) {
        this.verbose = verbose;
        this.debug = debug;
    }
    
    /**
     * Get or create the singleton instance
     */
    public static BuiltinVariableManager getInstance(boolean verbose, boolean debug) {
        if (instance == null) {
            instance = new BuiltinVariableManager(verbose, debug);
        }
        return instance;
    }
    
    /**
     * Set up all built-in variables in the script engine bindings
     * 
     * @param engine The ScriptEngine to set up variables for
     * @param scriptFile The script file being executed (can be null for interactive mode)
     * @param scriptArgs The arguments passed to the script (can be null)
     * @throws IOException if there are issues resolving paths
     */
    public void setupBuiltinVariables(ScriptEngine engine, String scriptFile, String[] scriptArgs) throws IOException {
        Bindings bindings = engine.createBindings();
        Map<String, Object> variables = createBuiltinVariables(scriptFile, scriptArgs);
        
        // Add all variables to bindings
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            bindings.put(entry.getKey(), entry.getValue());
        }
        
        // Set individual arguments for easy access (similar to CGI.argv)
        if (scriptArgs != null) {
            for (int i = 0; i < scriptArgs.length; i++) {
                bindings.put("arg" + (i + 1), scriptArgs[i]);
            }
        }
        
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        
        if (verbose) {
            System.out.println("Built-in script variables set:");
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                if (entry.getValue() instanceof String[]) {
                    System.out.println("  " + entry.getKey() + ": " + Arrays.toString((String[]) entry.getValue()));
                } else {
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                }
            }
        }
    }
    
    /**
     * Create a map of all built-in variables with their values
     * 
     * @param scriptFile The script file being executed (can be null for interactive mode)
     * @param scriptArgs The arguments passed to the script (can be null)
     * @return Map of variable names to values
     * @throws IOException if there are issues resolving paths
     */
    public Map<String, Object> createBuiltinVariables(String scriptFile, String[] scriptArgs) throws IOException {
        Map<String, Object> variables = new HashMap<>();
        Path effectiveCwd = LuCLI.getEffectiveRuntimeCwd();
        
        // Ensure we have empty array if no args provided
        if (scriptArgs == null) {
            scriptArgs = new String[0];
        }
        
        // Set script file information
        if (scriptFile != null) {
            variables.put(SCRIPT_FILE, scriptFile);
            variables.put(SCRIPT_PATH, Paths.get(scriptFile).toAbsolutePath().toString());
            
            // Handle case where file is in current directory (getParent() returns null)
            Path parent = Paths.get(scriptFile).getParent();
            String scriptDir = parent != null ? parent.toAbsolutePath().toString() : effectiveCwd.toString();
            variables.put(SCRIPT_DIR, scriptDir);
        } else {
            // For interactive mode or when no script file is specified
            variables.put(SCRIPT_FILE, "");
            variables.put(SCRIPT_PATH, "");
            variables.put(SCRIPT_DIR, effectiveCwd.toString());
        }
        
        // Set arguments
        variables.put(ARGUMENTS, scriptArgs);
        variables.put(ARGUMENT_COUNT, scriptArgs.length);
        variables.put(LEGACY_ARGV, createLegacyArgv(scriptFile, scriptArgs));
        variables.put(LEGACY_ARGS, createLegacyArgsStruct(scriptFile, scriptArgs));
        variables.put(NAMED_ARGUMENTS, createNamedArgumentMap(scriptArgs));
        
        // Environment variables.
        // For .lucli script execution, include dynamic values from LuCLI.scriptEnvironment
        // (e.g., set FOO=bar / source .env) layered over the process environment.
        Map<String, String> mergedEnv = new HashMap<>(System.getenv());
        if (LuCLI.scriptEnvironment != null && !LuCLI.scriptEnvironment.isEmpty()) {
            mergedEnv.putAll(LuCLI.scriptEnvironment);
        }
        variables.put(ENV, mergedEnv);
        
        // System properties
        variables.put(SYSTEM_PROPS, System.getProperties());
        
        // Add lucli home directory path for component mapping
        Path lucliHome = getLucliHomeDirectory();
        variables.put(LUCLI_HOME, lucliHome.toString());
        
        // Add LuCLI version
        variables.put(LUCLI_VERSION, LuCLI.getVersion());
        
        // Add current working directory
        variables.put(CURRENT_DIR, effectiveCwd.toString());
        
        return variables;
    }

    /**
     * Create legacy ARGV-style array with script name first, followed by raw CLI args.
     */
    public static String[] createLegacyArgv(String scriptFile, String[] scriptArgs) {
        if (scriptArgs == null) {
            scriptArgs = new String[0];
        }

        int offset = (scriptFile != null && !scriptFile.isEmpty()) ? 1 : 0;
        String[] argv = new String[scriptArgs.length + offset];
        int idx = 0;
        if (offset == 1) {
            argv[idx++] = scriptFile;
        }
        for (String arg : scriptArgs) {
            argv[idx++] = arg;
        }
        return argv;
    }

    /**
     * Create named argument map from CLI args. Supports key=value, --flag, and --no-flag.
     */
    public static Map<String, String> createNamedArgumentMap(String[] scriptArgs) {
        Map<String, String> named = new LinkedHashMap<>();
        if (scriptArgs == null) {
            return named;
        }

        for (String arg : scriptArgs) {
            if (arg == null) {
                continue;
            }

            int equalsIndex = arg.indexOf('=');
            if (equalsIndex > 0) {
                String rawKey = arg.substring(0, equalsIndex).trim();
                String value = arg.substring(equalsIndex + 1);
                String normalizedKey = normalizeArgumentKey(rawKey);
                if (!normalizedKey.isEmpty()) {
                    named.put(normalizedKey, value);
                }
                continue;
            }

            if (arg.startsWith("--no-") && arg.length() > 5) {
                String normalizedKey = normalizeArgumentKey(arg.substring(5));
                if (!normalizedKey.isEmpty()) {
                    named.put(normalizedKey, "false");
                }
                continue;
            }

            if (arg.startsWith("--") && arg.length() > 2) {
                String normalizedKey = normalizeArgumentKey(arg.substring(2));
                if (!normalizedKey.isEmpty()) {
                    named.put(normalizedKey, "true");
                }
            }
        }

        return named;
    }

    /**
     * Create legacy ARGS struct:
     * - numeric keys (1-based) for positional compatibility
     * - named keys for key=value style access (ARGS["name"])
     */
    public static Map<String, Object> createLegacyArgsStruct(String scriptFile, String[] scriptArgs) {
        Map<String, Object> args = new LinkedHashMap<>();
        String[] argv = createLegacyArgv(scriptFile, scriptArgs);
        for (int i = 0; i < argv.length; i++) {
            args.put(String.valueOf(i + 1), argv[i]);
        }

        Map<String, String> named = createNamedArgumentMap(scriptArgs);
        for (Map.Entry<String, String> entry : named.entrySet()) {
            args.put(entry.getKey(), entry.getValue());
        }

        return args;
    }

    private static String normalizeArgumentKey(String key) {
        if (key == null) {
            return "";
        }

        String normalized = key.trim();
        if (normalized.startsWith("--")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }

        return normalized.trim();
    }
    
    /**
     * Create a CFML script that sets up all built-in variables as CFML variables
     * This is useful for script templates that need variables available as CFML vars
     * 
     * @param scriptFile The script file being executed (can be null for interactive mode)
     * @param scriptArgs The arguments passed to the script (can be null)
     * @return CFML script string that sets up all variables
     * @throws IOException if there are issues resolving paths
     */
    public String createVariableSetupScript(String scriptFile, String[] scriptArgs) throws IOException {
        Map<String, Object> variables = createBuiltinVariables(scriptFile, scriptArgs);
        StringBuilder script = new StringBuilder();
        
        script.append("// LuCLI Built-in Variables Setup\n");
        script.append("// This script sets up all built-in variables for CFML scripts\n\n");
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String varName = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                // Escape single quotes in string values
                String escapedValue = ((String) value).replace("'", "''");
                script.append(varName).append(" = '").append(escapedValue).append("';\n");
            } else if (value instanceof String[]) {
                // Handle string arrays
                String[] arrayValues = (String[]) value;
                script.append(varName).append(" = [];\n");
                for (String arrayValue : arrayValues) {
                    String escapedValue = arrayValue.replace("'", "''");
                    script.append("arrayAppend(").append(varName).append(", '").append(escapedValue).append("');\n");
                }
            } else if (value instanceof Integer) {
                script.append(varName).append(" = ").append(value).append(";\n");
            } else {
                // For complex objects like Maps, we'll serialize them or handle them specially
                if (varName.equals(ENV) || varName.equals(SYSTEM_PROPS) || varName.equals(LEGACY_ARGS)
                        || varName.equals(NAMED_ARGUMENTS)) {
                    // These are handled specially by the ScriptEngine bindings
                    script.append("// ").append(varName).append(" is available as a complex object\n");
                } else {
                    script.append(varName).append(" = '").append(value.toString().replace("'", "''")).append("';\n");
                }
            }
        }
        
        // Add individual argument variables
        if (scriptArgs != null) {
            script.append("\n// Individual argument variables (arg1, arg2, etc.)\n");
            for (int i = 0; i < scriptArgs.length; i++) {
                String escapedValue = scriptArgs[i].replace("'", "''");
                script.append("arg").append(i + 1).append(" = '").append(escapedValue).append("';\n");
            }
        }
        
        script.append("\n");
        return script.toString();
    }
    
    /**
     * Get the LuCLI home directory
     */
    private Path getLucliHomeDirectory() {
        String lucliHome = System.getProperty("lucli.home");
        if (lucliHome != null) {
            return Paths.get(lucliHome);
        }
        
        String envHome = System.getenv("LUCLI_HOME");
        if (envHome != null) {
            return Paths.get(envHome);
        }
        
        return Paths.get(System.getProperty("user.home"), ".lucli");
    }
    
    /**
     * Check if verbose mode is enabled
     */
    public boolean isVerboseMode() {
        return verbose;
    }
    
    /**
     * Check if debug mode is enabled
     */
    public boolean isDebugMode() {
        return debug;
    }
}