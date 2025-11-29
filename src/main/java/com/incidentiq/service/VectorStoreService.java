package com.incidentiq.service;

import com.incidentiq.model.LogChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final JedisPooled jedis;
    private final EmbeddingService embeddingService;

    private static final String PREFIX = "emb:";

    public void storeEmbedding(LogChunk chunk) {
        byte[] vectorBytes = embeddingService.getEmbeddingAsBytes(chunk.getText());
        store(chunk, vectorBytes);
    }

    public void store(LogChunk chunk, byte[] embeddingBytes) {
        String redisKey = PREFIX + chunk.getId();

        try {
            jedis.hset(redisKey, "text", chunk.getText());
            jedis.hset(redisKey, "source", chunk.getSource());
            jedis.hset(redisKey, "fileName", chunk.getFileName());
            jedis.hset(redisKey, "chunkNo", String.valueOf(chunk.getChunkNo()));

            jedis.hset(redisKey.getBytes(), "vector".getBytes(), embeddingBytes);

            log.info("Stored embedding for key {}", redisKey);
        } catch (Exception e) {
            log.error("Failed to store embedding for key {}", redisKey, e);
            throw new IllegalStateException("Failed to store embedding", e);
        }
    }
}
