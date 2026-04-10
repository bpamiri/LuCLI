package org.lucee.lucli.profile;

/**
 * Default LuCLI profile — used when invoked as {@code lucli} or any
 * unrecognised binary name.
 */
public class DefaultProfile implements CliProfile {

    @Override
    public String name() {
        return "lucli";
    }

    @Override
    public String homeDirName() {
        return ".lucli";
    }

    @Override
    public String promptPrefix() {
        return "cfml";
    }

    @Override
    public String displayName() {
        return "LuCLI";
    }

    @Override
    public String bannerText() {
        return " _           ____ _     ___ \n"
             + "| |   _   _ / ___| |   |_ _|\n"
             + "| |  | | | | |   | |    | | \n"
             + "| |__| |_| | |___| |___ | | \n"
             + "|_____\\__,_|\\____|_____|___|\n";
    }
}
