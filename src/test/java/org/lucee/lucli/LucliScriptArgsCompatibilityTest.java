package org.lucee.lucli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LucliScriptArgsCompatibilityTest {

    @TempDir
    Path tempDir;

    @Test
    void cfsNamedArgumentsAreAvailableInArgsStruct() throws Exception {
        Path cfsFile = tempDir.resolve("named_args.cfs");
        Files.writeString(
            cfsFile,
            "result = serializeJSON({namedArg1=ARGS[\"arg1\"], namedName=ARGS[\"name\"], pos2=ARGS[2], pos3=ARGS[3], argvIsArray=isArray(ARGV), argvLen=arrayLen(ARGV)});",
            StandardCharsets.UTF_8
        );

        LuceeScriptEngine engine = LuceeScriptEngine.getInstance();
        engine.executeCFSFile(cfsFile.toString(), new String[] { "arg1=elvis", "name=myName" });
        String output = String.valueOf(engine.getEngine().get("result"));

        assertTrue(output.contains("\"namedArg1\":\"elvis\""), "Expected ARGS[\"arg1\"] to be available in .cfs");
        assertTrue(output.contains("\"namedName\":\"myName\""), "Expected ARGS[\"name\"] to be available in .cfs");
        assertTrue(output.contains("\"pos2\":\"arg1=elvis\""), "Expected positional numeric key compatibility in ARGS");
        assertTrue(output.contains("\"pos3\":\"name=myName\""), "Expected positional numeric key compatibility in ARGS");
        assertTrue(output.contains("\"argvIsArray\":true"), "Expected ARGV to remain an array");
        assertTrue(output.contains("\"argvLen\":3"), "Expected ARGV to include script name + raw args");
    }

    @Test
    void cfmNamedArgumentsAreAvailableInArgsStruct() throws Exception {
        Path cfmFile = tempDir.resolve("named_args.cfm");
        Files.writeString(
            cfmFile,
            "<cfset result = serializeJSON({namedArg1=ARGS[\"arg1\"], namedName=ARGS[\"name\"], pos2=ARGS[2], pos3=ARGS[3], argvIsArray=isArray(ARGV), argvLen=arrayLen(ARGV)})>",
            StandardCharsets.UTF_8
        );

        LuceeScriptEngine engine = LuceeScriptEngine.getInstance();
        engine.executeCFMFile(cfmFile.toString(), new String[] { "arg1=elvis", "name=myName" });
        String output = String.valueOf(engine.getEngine().get("result"));

        assertTrue(output.contains("\"namedArg1\":\"elvis\""), "Expected ARGS[\"arg1\"] to be available in .cfm");
        assertTrue(output.contains("\"namedName\":\"myName\""), "Expected ARGS[\"name\"] to be available in .cfm");
        assertTrue(output.contains("\"pos2\":\"arg1=elvis\""), "Expected positional numeric key compatibility in ARGS");
        assertTrue(output.contains("\"pos3\":\"name=myName\""), "Expected positional numeric key compatibility in ARGS");
        assertTrue(output.contains("\"argvIsArray\":true"), "Expected ARGV to remain an array");
        assertTrue(output.contains("\"argvLen\":3"), "Expected ARGV to include script name + raw args");
    }
}
