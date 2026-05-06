package org.lucee.lucli.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

public class AiCommandConfigPayloadTest {

    @Test
    void buildAiConfigImportPayload_claudeTypeUsesClaudeEngineAndApiKey() {
        AiCommand.AddConfigRequest request = baseRequest("Claude");
        request.type = "claude";
        request.model = "claude-opus-4-6";
        request.url = "https://api.anthropic.com/v1/";

        ObjectNode payload = AiCommand.buildAiConfigImportPayload(request);

        JsonNode entry = payload.path("ai").path("Claude");
        JsonNode custom = entry.path("custom");

        assertEquals("lucee.runtime.ai.anthropic.ClaudeEngine", entry.path("class").asText());
        assertEquals(request.secretKey, custom.path("apiKey").asText());
        assertEquals(5000, custom.path("timeout").asInt());
        assertTrue(custom.path("secretKey").isMissingNode());
        assertTrue(custom.path("type").isMissingNode());
    }

    @Test
    void buildAiConfigImportPayload_geminiTypeUsesGeminiTemplate() {
        AiCommand.AddConfigRequest request = baseRequest("Gemini");
        request.type = "gemini";
        request.model = "gemini-3.1-pro-preview";
        request.secretKey = "TEST_GEMINI_API_KEY";

        ObjectNode payload = AiCommand.buildAiConfigImportPayload(request);

        JsonNode entry = payload.path("ai").path("Gemini");
        JsonNode custom = entry.path("custom");

        assertEquals("lucee.runtime.ai.google.GeminiEngine", entry.path("class").asText());
        assertEquals("2000", custom.path("connectTimeout").asText());
        assertEquals("true", custom.path("beta").asText());
        assertEquals("", custom.path("message").asText());
        assertEquals("gemini-3.1-pro-preview", custom.path("model").asText());
        assertEquals("0.7", custom.path("temperature").asText());
        assertEquals("TEST_GEMINI_API_KEY", custom.path("apikey").asText());
        assertEquals("20000", custom.path("socketTimeout").asText());
        assertEquals("100", custom.path("conversationSizeLimit").asText());
        assertEquals(8, custom.size());
        assertTrue(custom.path("apiKey").isMissingNode());
        assertTrue(custom.path("secretKey").isMissingNode());
        assertTrue(custom.path("timeout").isMissingNode());
        assertTrue(custom.path("url").isMissingNode());
        assertTrue(custom.path("type").isMissingNode());
        assertTrue(entry.path("default").isMissingNode());
    }

    @Test
    void buildAiConfigImportPayload_openAiTypeUsesSecretKeyAndType() {
        AiCommand.AddConfigRequest request = baseRequest("OpenAI");
        request.type = "openai";
        request.model = "gpt-4o";

        ObjectNode payload = AiCommand.buildAiConfigImportPayload(request);

        JsonNode entry = payload.path("ai").path("OpenAI");
        JsonNode custom = entry.path("custom");

        assertEquals("lucee.runtime.ai.openai.OpenAIEngine", entry.path("class").asText());
        assertEquals(request.secretKey, custom.path("secretKey").asText());
        assertEquals("openai", custom.path("type").asText());
        assertTrue(custom.path("apiKey").isMissingNode());
        assertTrue(custom.path("timeout").isMissingNode());
    }

    @Test
    void buildAiConfigImportPayload_claudeEngineClassUsesApiKeyWhenTypeMissing() {
        AiCommand.AddConfigRequest request = baseRequest("ClaudeByClass");
        request.className = "lucee.runtime.ai.anthropic.ClaudeEngine";
        request.type = null;
        request.model = "claude-opus-4-6";

        ObjectNode payload = AiCommand.buildAiConfigImportPayload(request);

        JsonNode entry = payload.path("ai").path("ClaudeByClass");
        JsonNode custom = entry.path("custom");

        assertEquals("lucee.runtime.ai.anthropic.ClaudeEngine", entry.path("class").asText());
        assertEquals(request.secretKey, custom.path("apiKey").asText());
        assertTrue(custom.path("secretKey").isMissingNode());
        assertTrue(custom.path("type").isMissingNode());
    }

    private AiCommand.AddConfigRequest baseRequest(String name) {
        AiCommand.AddConfigRequest request = new AiCommand.AddConfigRequest();
        request.name = name;
        request.secretKey = "#secret:PERSONAL_KEY#";
        request.message = "Keep all answers as short as possible";
        request.timeout = 5000;
        request.defaultMode = "exception";
        return request;
    }
}
