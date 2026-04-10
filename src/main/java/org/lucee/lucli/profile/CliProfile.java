package org.lucee.lucli.profile;

/**
 * Defines branding and directory conventions for a CLI binary identity.
 *
 * <p>LuCLI can be invoked under different binary names (e.g. {@code lucli},
 * {@code wheels}) via symlinks or the {@code -Dlucli.binary.name} system
 * property. Each binary name maps to a profile that controls the home
 * directory name, ASCII art banner, and interactive prompt prefix.</p>
 */
public interface CliProfile {

    /** Short identifier for this profile, e.g. {@code "lucli"} or {@code "wheels"}. */
    String name();

    /** Directory name under the user's home for caches/config, e.g. {@code ".lucli"}. */
    String homeDirName();

    /** ASCII art banner displayed in {@code --version} output. */
    String bannerText();

    /** Prefix shown in interactive prompts, e.g. {@code "cfml"} or {@code "wheels"}. */
    String promptPrefix();

    /** Human-readable display name for version output, e.g. {@code "LuCLI"} or {@code "Wheels"}. */
    String displayName();

    /**
     * Resolve the appropriate profile for the given binary name.
     *
     * @param binaryName the name the CLI was invoked as (may be {@code null})
     * @return a {@link WheelsProfile} when the binary name is {@code "wheels"}
     *         (case-insensitive), otherwise a {@link DefaultProfile}
     */
    static CliProfile forBinaryName(String binaryName) {
        if (binaryName != null && binaryName.equalsIgnoreCase("wheels")) {
            return new WheelsProfile();
        }
        return new DefaultProfile();
    }
}
