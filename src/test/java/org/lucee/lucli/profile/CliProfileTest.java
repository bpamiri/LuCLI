package org.lucee.lucli.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CliProfileTest {

    @Test
    void forBinaryName_returnsWheelsProfileForWheels() {
        CliProfile profile = CliProfile.forBinaryName("wheels");
        assertInstanceOf(WheelsProfile.class, profile);
        assertEquals("wheels", profile.name());
        assertEquals(".wheels", profile.homeDirName());
        assertEquals("wheels", profile.promptPrefix());
        assertEquals("Wheels", profile.displayName());
    }

    @Test
    void forBinaryName_returnsWheelsProfileCaseInsensitive() {
        CliProfile profile = CliProfile.forBinaryName("Wheels");
        assertInstanceOf(WheelsProfile.class, profile);
        assertEquals("wheels", profile.name());
    }

    @Test
    void forBinaryName_returnsDefaultProfileForLucli() {
        CliProfile profile = CliProfile.forBinaryName("lucli");
        assertInstanceOf(DefaultProfile.class, profile);
        assertEquals("lucli", profile.name());
        assertEquals(".lucli", profile.homeDirName());
        assertEquals("cfml", profile.promptPrefix());
        assertEquals("LuCLI", profile.displayName());
    }

    @Test
    void forBinaryName_returnsDefaultProfileForNull() {
        CliProfile profile = CliProfile.forBinaryName(null);
        assertInstanceOf(DefaultProfile.class, profile);
    }

    @Test
    void forBinaryName_returnsDefaultProfileForUnknown() {
        CliProfile profile = CliProfile.forBinaryName("someothertool");
        assertInstanceOf(DefaultProfile.class, profile);
    }

    @Test
    void defaultProfile_bannerContainsLuCLI() {
        DefaultProfile profile = new DefaultProfile();
        // The banner should contain recognizable LuCLI ASCII art
        assertTrue(profile.bannerText().contains("___ "));
    }

    @Test
    void wheelsProfile_bannerContainsWheels() {
        WheelsProfile profile = new WheelsProfile();
        // The banner should contain recognizable Wheels ASCII art
        assertTrue(profile.bannerText().contains("\\__ \\"));
    }
}
