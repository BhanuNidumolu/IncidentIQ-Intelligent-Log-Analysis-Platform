package com.incidentiq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IncidentInsightService {

    private final SearchService searchService;
    private final GeminiChatClient chatClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public IncidentInsightService(SearchService searchService, GeminiChatClient chatClient) {
        this.searchService = searchService;
        this.chatClient = chatClient;
    }

    public Map<String, Object> generateInsight(String query) throws Exception {

        // ------------------------------------
        // 1) SEMANTIC SEARCH
        // ------------------------------------
        List<VectorStoreService.SearchResult> results =
                searchService.search(query, 5);

        // Build context from similar logs
        StringBuilder ctx = new StringBuilder();
        for (var r : results) {
            ctx.append("- ").append(r.text()).append("\n");
        }

        // ------------------------------------
        // 2) STRICT JSON PROMPT
        // ------------------------------------
        String prompt = """
                You are a Senior SRE. Your job is to do RCA from logs.

                ---- LOGS ----
                %s

                ---- USER QUERY ----
                %s

                You MUST reply ONLY in JSON.
                NO markdown, NO explanation, NO text outside JSON.
                If you cannot determine a field, set it to null.

                JSON FORMAT:
                {
                  "rootCause": "...",
                  "impact": "...",
                  "recommendation": "...",
                  "confidence": 0.0,
                  "summary": "..."
                }
                """.formatted(ctx.toString(), query);

        // ------------------------------------
        // 3) CALL GEMINI
        // ------------------------------------
        String llmOutput = chatClient.chat(prompt);

        // Remove any accidental formatting
        llmOutput = llmOutput
                .replace("```json", "")
                .replace("```", "")
                .trim();

        // ------------------------------------
        // 4) PARSE JSON SAFELY
        // ------------------------------------
        Map<String, Object> parsed;

        try {
            parsed = mapper.readValue(llmOutput, Map.class);

        } catch (Exception ex) {
            // Hard fallback: pack entire raw output in a JSON-safe response
            parsed = new HashMap<>();
            parsed.put("summary", "Model did not return valid JSON.");
            parsed.put("rootCause", "Gemini returned unexpected format.");
            parsed.put("impact", "Unknown");
            parsed.put("recommendation", "Retry with clearer logs.");
            parsed.put("confidence", 0.0);
            parsed.put("rawResponse", llmOutput); // helpful for debugging
        }

        // ------------------------------------
        // 5) FINAL OUTPUT
        // ------------------------------------
        Map<String, Object> output = new HashMap<>();
        output.put("relatedLogs", results);
        output.put("analysis", parsed);

        return output;
    }
}
