package com.incidentiq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class GeminiChatClient {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.completions-path}")
    private String completionsPath;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    public String chat(String prompt) throws Exception {

        String url = baseUrl + completionsPath;

        // Build request JSON
        JsonNode contentNode = mapper.createArrayNode()
                .add(mapper.createObjectNode()
                        .put("type", "text")
                        .put("text", prompt));

        JsonNode messageNode = mapper.createObjectNode()
                .put("role", "user")
                .set("content", contentNode);

        JsonNode root = mapper.createObjectNode()
                .put("model", model)
                .set("messages", mapper.createArrayNode().add(messageNode));

        String bodyJson = mapper.writeValueAsString(root);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("User-Agent", "IncidentIQ-Client/1.0")
                .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (response.body() == null) {
                throw new RuntimeException("Gemini returned empty body");
            }

            String responseStr = response.body().string();

            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        "Gemini Chat Error → HTTP " + response.code() + "\n" + responseStr
                );
            }

            JsonNode json = mapper.readTree(responseStr);

            // -------------- SAFE PARSING --------------
            JsonNode choices = json.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                return "[Gemini returned no choices → Raw response: " + responseStr + "]";
            }

            JsonNode message = choices.get(0).path("message");
            if (message.isMissingNode()) {
                return "[Gemini returned no message → Raw response: " + responseStr + "]";
            }

            JsonNode content = message.path("content");
            if (!content.isArray() || content.size() == 0) {
                return "[Gemini returned empty content → Raw response: " + responseStr + "]";
            }

            JsonNode textNode = content.get(0).path("text");
            if (textNode.isMissingNode()) {
                return "[Gemini returned no text → Raw response: " + responseStr + "]";
            }

            return textNode.asText();
        }
    }
}
