package com.incidentiq.service;

import com.incidentiq.model.SearchHit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RediSearchKnnService {

    private final JedisPooled jedis;
    private final EmbeddingService embeddingService;

    private static final String INDEX = "idx:logs";

    public List<SearchHit> knnSearch(String query, int topK) {
        byte[] vec = embeddingService.embedToBinary(query);

        String knnExpr = "*=>[KNN " + topK + " @vector $vec AS score]";

        Query q = new Query(knnExpr)
                .addParam("vec", vec)
                .returnFields("text", "fileName", "score")
                .setSortBy("score", true)   // for COSINE distance (lower = closer)
                .dialect(2);

        try {
            SearchResult result = jedis.ftSearch(INDEX, q);
            return convert(result);
        } catch (Exception e) {
            log.error("KNN search error", e);
            return List.of();
        }
    }

    public List<SearchHit> hybridKnnSearch(String query, String filter, int topK) {
        byte[] vec = embeddingService.embedToBinary(query);

        String baseExpr;
        if (filter == null || filter.isBlank()) {
            baseExpr = "*";
        } else {
            baseExpr = "(" + filter + ")";
        }

        String searchExpr = baseExpr + "=>[KNN " + topK + " @vector $vec AS score]";

        Query q = new Query(searchExpr)
                .addParam("vec", vec)
                .returnFields("text", "fileName", "score")
                .setSortBy("score", true)
                .dialect(2);

        try {
            SearchResult result = jedis.ftSearch(INDEX, q);
            return convert(result);
        } catch (Exception e) {
            log.error("Hybrid KNN search error", e);
            return List.of();
        }
    }

    private List<SearchHit> convert(SearchResult sr) {
        List<SearchHit> out = new ArrayList<>();

        for (Document d : sr.getDocuments()) {
            SearchHit hit = new SearchHit();
            hit.setId(d.getId());
            hit.setText((String) d.get("text"));
            hit.setScore(parse(d.get("score")));

            Object fileVal = d.get("fileName");
            hit.setMeta(fileVal != null ? fileVal.toString() : "");

            out.add(hit);
        }
        return out;
    }

    private double parse(Object v) {
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
