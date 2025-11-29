package com.incidentiq.config;

import com.incidentiq.service.EmbeddingService;
import com.incidentiq.util.RedisSearchCommand;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisVectorInitializer {

    private final JedisPooled jedis;
    private final EmbeddingService embeddingService;

    private static final String INDEX_NAME = "idx:logs";
    private static final String PREFIX = "emb:";

    @PostConstruct
    public void init() {
        String indexName = INDEX_NAME;

        // Check if index already exists
        try {
            jedis.sendCommand(RedisSearchCommand.FT_INFO, indexName);
            log.info("RediSearch index '{}' already exists", indexName);
            return;
        } catch (Exception e) {
            log.info("Index '{}' not found, creating...", indexName);
        }

        int dim = embeddingService.getEmbeddingDim();

        try {
            // FT.CREATE idx:logs ON HASH PREFIX 1 emb: SCHEMA text TEXT fileName TAG SEPARATOR "|" vector VECTOR FLAT 6 TYPE FLOAT32 DIM {dim} DISTANCE_METRIC COSINE
            jedis.sendCommand(
                    RedisSearchCommand.FT_CREATE,
                    INDEX_NAME,
                    "ON", "HASH",
                    "PREFIX", "1", PREFIX,
                    "SCHEMA",
                    "text", "TEXT",
                    "fileName", "TAG", "SEPARATOR", "|",
                    "vector", "VECTOR", "FLAT", "6",
                    "TYPE", "FLOAT32",
                    "DIM", String.valueOf(dim),
                    "DISTANCE_METRIC", "COSINE"
            );

            log.info("RediSearch vector index '{}' created successfully with DIM={}", indexName, dim);
        } catch (Exception e) {
            log.error("Failed to create RediSearch index '{}'", indexName, e);
            throw new IllegalStateException("Could not create Redis vector index", e);
        }
    }
}
