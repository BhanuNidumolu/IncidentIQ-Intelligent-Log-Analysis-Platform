package com.incidentiq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    private static final String DEFAULT_PREFIX = "emb:";

    private final JedisPooled jedis;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String prefix;

    public VectorStoreService(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${app.redis.prefix:" + DEFAULT_PREFIX + "}") String prefix
    ) {
        this.jedis = new JedisPooled(host, port);
        this.prefix = prefix == null ? DEFAULT_PREFIX : prefix;
    }

    /**
     * Upsert document as a Redis HASH:
     * HSET <prefix><id> text <text> meta <json> vector <base64>
     */
    public String upsert(String id, float[] vector, String text, Map<String, Object> metadata) {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();

        normalizeL2InPlace(vector);
        byte[] vecBytes = floatsToBytesLE(vector);
        String vecBase64 = Base64.getEncoder().encodeToString(vecBytes);

        if (metadata == null) metadata = new HashMap<>();
        metadata.putIfAbsent("timestamp", Instant.now().toString());

        String key = prefix + id;

        try {
            Map<String, String> flat = new HashMap<>();
            flat.put("text", text == null ? "" : text);
            flat.put("meta", mapper.writeValueAsString(metadata));
            flat.put("vector", vecBase64);
            jedis.hset(key, flat);
            return id;
        } catch (Exception e) {
            throw new RuntimeException("upsert failed: " + e.getMessage(), e);
        }
    }

    public Optional<StoredDoc> getById(String id) {
        String key = prefix + id;
        Map<String, String> map = jedis.hgetAll(key);
        if (map == null || map.isEmpty()) return Optional.empty();
        try {
            String text = map.getOrDefault("text", "");
            String vecBase64 = map.getOrDefault("vector", "");
            byte[] vecBytes = vecBase64.isBlank() ? new byte[0] : Base64.getDecoder().decode(vecBase64);
            float[] vec = bytesToFloatsLE(vecBytes);
            Map<String,Object> meta = new HashMap<>();
            String metaJson = map.getOrDefault("meta", "{}");
            try { meta = mapper.readValue(metaJson, Map.class); } catch (Exception ignore) {}
            return Optional.of(new StoredDoc(id, text, vec, meta));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Java-side KNN fallback: scan keys, compute cosine similarity against base64 vector stored in hash.
     */
    public List<SearchResult> searchKnn(float[] queryEmbedding, int k) {
        if (queryEmbedding == null || queryEmbedding.length == 0) return List.of();
        float[] q = Arrays.copyOf(queryEmbedding, queryEmbedding.length);
        normalizeL2InPlace(q);

        List<SearchResult> results = new ArrayList<>();

        Set<String> keys = jedis.keys(prefix + "*");
        for (String key : keys) {
            try {
                Map<String, String> map = jedis.hgetAll(key);
                if (map == null || map.isEmpty()) continue;
                String text = map.getOrDefault("text", "");
                String vecBase64 = map.getOrDefault("vector", "");
                if (vecBase64.isBlank()) continue;
                byte[] vecBytes = Base64.getDecoder().decode(vecBase64);
                float[] v = bytesToFloatsLE(vecBytes);
                int n = Math.min(q.length, v.length);
                double score = cosineSimilarity(q, v, n);
                String id = key.startsWith(prefix) ? key.substring(prefix.length()) : key;
                results.add(new SearchResult(id, text, score));
            } catch (Exception ignore) {
            }
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(k)
                .collect(Collectors.toList());
    }

    public void deleteById(String id) {
        jedis.del(prefix + id);
    }

    // ---------- helpers ----------
    private static byte[] floatsToBytesLE(float[] v) {
        ByteBuffer bb = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) bb.putFloat(f);
        return bb.array();
    }

    private static float[] bytesToFloatsLE(byte[] b) {
        if (b == null || b.length == 0) return new float[0];
        int n = b.length / 4;
        float[] out = new float[n];
        ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; ++i) out[i] = bb.getFloat(i * 4);
        return out;
    }

    private static void normalizeL2InPlace(float[] v) {
        if (v == null || v.length == 0) return;
        double sum = 0;
        for (float x : v) sum += (double)x * x;
        double norm = Math.sqrt(sum);
        if (norm == 0) return;
        for (int i = 0; i < v.length; ++i) v[i] = (float)(v[i] / norm);
    }

    private static double cosineSimilarity(float[] a, float[] b, int n) {
        double dot=0, na=0, nb=0;
        for (int i=0;i<n;i++){ dot += a[i]*b[i]; na += a[i]*a[i]; nb += b[i]*b[i];}
        if (na==0 || nb==0) return 0.0;
        return dot / (Math.sqrt(na)*Math.sqrt(nb));
    }

    public record SearchResult(String id, String text, double score) {}
    public record StoredDoc(String id, String text, float[] vector, Map<String,Object> meta) {}
}
