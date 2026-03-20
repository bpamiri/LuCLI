package org.lucee.lucli.cli.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

class AiCommandTest {

    @Test
    void configAddHelpIncludesQuietOption() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            new CommandLine(new AiCommand.ConfigCommand.AddCommand()).usage(System.out);
        } finally {
            System.setOut(originalOut);
        }

        String usage = out.toString(StandardCharsets.UTF_8);
        assertTrue(usage.contains("--quiet"));
        assertTrue(usage.contains("Suppress printing imported config payload"));
    }

    @Test
    void configAddParsesQuietOption() {
        ParseResult parseResult = new CommandLine(new AiCommand.ConfigCommand.AddCommand()).parseArgs("--quiet");
        assertTrue(parseResult.hasMatchedOption("--quiet"));
    }
}
