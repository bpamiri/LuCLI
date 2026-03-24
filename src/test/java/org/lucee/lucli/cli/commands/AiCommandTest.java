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

    @Test
    void promptHelpIncludesEstimateOptions() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            new CommandLine(new AiCommand.PromptCommand()).usage(System.out);
        } finally {
            System.setOut(originalOut);
        }

        String usage = out.toString(StandardCharsets.UTF_8);
        assertTrue(usage.contains("--estimate"));
        assertTrue(usage.contains("exit without"));
        assertTrue(usage.contains("sending request"));
        assertTrue(usage.contains("--assume-output-tokens"));
        assertTrue(usage.contains("--input-price-per-1m"));
        assertTrue(usage.contains("--output-price-per-1m"));
    }

    @Test
    void promptParsesEstimateOptions() {
        ParseResult parseResult = new CommandLine(new AiCommand.PromptCommand()).parseArgs(
            "--estimate",
            "--assume-output-tokens=512",
            "--input-price-per-1m=2.50",
            "--output-price-per-1m=10.00"
        );
        assertTrue(parseResult.hasMatchedOption("--estimate"));
        assertTrue(parseResult.hasMatchedOption("--assume-output-tokens"));
        assertTrue(parseResult.hasMatchedOption("--input-price-per-1m"));
        assertTrue(parseResult.hasMatchedOption("--output-price-per-1m"));
    }
}
