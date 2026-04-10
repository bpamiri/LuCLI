package org.lucee.lucli.profile;

/**
 * Wheels profile — activated when the CLI is invoked as {@code wheels}.
 * Uses Wheels-specific branding and a {@code ~/.wheels} home directory.
 */
public class WheelsProfile implements CliProfile {

    @Override
    public String name() {
        return "wheels";
    }

    @Override
    public String homeDirName() {
        return ".wheels";
    }

    @Override
    public String promptPrefix() {
        return "wheels";
    }

    @Override
    public String displayName() {
        return "Wheels";
    }

    @Override
    public String bannerText() {
        return " __        ___               _     \n"
             + " \\ \\      / / |__   ___  ___| |___ \n"
             + "  \\ \\ /\\ / /| '_ \\ / _ \\/ _ \\ / __|\n"
             + "   \\ V  V / | | | |  __/  __/ \\__ \\\n"
             + "    \\_/\\_/  |_| |_|\\___|\\___|_|___/\n";
    }
}
