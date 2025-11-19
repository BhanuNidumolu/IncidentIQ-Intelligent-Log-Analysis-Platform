package com.incidentiq.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

@Component
public class GeminiEmbeddingClient {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private static final String EMBED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=%s";

    public float[] embed(String text) {

        RestTemplate rest = new RestTemplate();

        // ✔ CORRECT Google Gemini embedContent format
        Map<String, Object> body = Map.of(
                "content", Map.of(
                        "parts", List.of(
                                Map.of("text", text)
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        String url = EMBED_URL.formatted(apiKey);

        // Execute API call
        Map resp = rest.postForObject(url, req, Map.class);

        // Response → { "embedding": { "values": [...] } }
        Map embedding = (Map) resp.get("embedding");
        List<Double> values = (List<Double>) embedding.get("values");

        float[] out = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i).floatValue();
        }

        return out;
    }

    public byte[] toFloat32Bytes(float[] vector) {
        ByteBuffer buf = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : vector) buf.putFloat(v);
        return buf.array();
    }

    public int getConfiguredDim() {
        return 768; // gemini-embedding-001 output size
    }
}
