package com.incidentiq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.model.RootCauseInsight;
import com.incidentiq.model.RootCauseInsight.EvidenceHit;
import com.incidentiq.model.SearchHit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentInsightService {

    private final SearchService semanticSearchService;
    private final GeminiChatClient geminiChatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RootCauseInsight analyzeRootCause(String query, int topK) {

        // 1) Evidence collection
        List<SearchHit> hits = semanticSearchService.hybridSearch(query, topK);

        List<EvidenceHit> evidence = hits.stream()
                .map(h -> EvidenceHit.builder()
                        .id(h.getId())
                        .text(h.getText())
                        .meta(h.getMeta())
                        .score(h.getScore())
                        .build())
                .collect(Collectors.toList());

        String context = evidence.stream()
                .map(e -> "### Evidence (score=" + e.getScore() + ")\n" + e.getText())
                .collect(Collectors.joining("\n\n"));

        // 2) Prompt
        String prompt = """
                You are an expert SRE / DevOps AI assistant.

                User Query:
                %s

                Evidence Logs:
                %s

                Provide a STRICT JSON response ONLY:
                {
                  "summary": "...",
                  "root_cause": "...",
                  "impact": "...",
                  "actions": "...",
                  "confidence": "HIGH/MEDIUM/LOW"
                }

                DO NOT wrap the response in ```json blocks.
                """.formatted(query, context);

        // 3) Call Gemini
        String raw;
        try {
            raw = geminiChatClient.chat(prompt);
        } catch (Exception ex) {
            log.error("AI root cause analysis failed for query='{}'", query, ex);
            throw new RuntimeException("AI Analysis failed: " + ex.getMessage());
        }

        // 4) Clean + Parse LLM JSON
        JsonNode json = safeJson(raw);

        String summary = safeField(json, "summary");
        String root = safeField(json, "root_cause");
        String impact = safeField(json, "impact");
        String actions = safeField(json, "actions");
        String confidence = safeField(json, "confidence");

        // 5) Build response
        return RootCauseInsight.builder()
                .query(query)
                .rootCauseSummary(summary)
                .probableRootCause(root)
                .impact(impact)
                .recommendedActions(actions)
                .confidenceLevel(confidence)
                .rawAnalysis(raw)
                .evidence(evidence)
                .build();
    }

    /**
     * Removes Markdown fences (```json ... ```) before JSON parsing.
     */
    private JsonNode safeJson(String str) {
        try {
            String cleaned = str
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            log.info("Cleaned AI JSON: {}", cleaned);

            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.warn("AI returned invalid JSON. Raw returned string: {}", str);
            return objectMapper.createObjectNode();
        }
    }

    /**
     * Safely extracts field from JSON without crashing.
     */
    private String safeField(JsonNode json, String field) {
        try {
            if (json.has(field)) {
                return json.get(field).asText();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
