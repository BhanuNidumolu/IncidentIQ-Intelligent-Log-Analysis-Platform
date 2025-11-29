package com.incidentiq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.model.LogChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogChunkService {

    private final JedisPooled jedis;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String QUEUE_KEY = "ingest_queue";

    public List<LogChunk> chunk(String source, String fileName, String text) {
        LogChunk c = new LogChunk();
        c.setId(UUID.randomUUID().toString());
        c.setSource(source);
        c.setFileName(fileName);
        c.setChunkNo(0);
        c.setText(text);

        log.info("Chunk created: {} for source={} fileName={}", c.getId(), source, fileName);
        return List.of(c);
    }

    public void enqueue(LogChunk chunk) {
        try {
            String json = mapper.writeValueAsString(chunk);
            Long size = jedis.lpush(QUEUE_KEY, json);
            log.info("Enqueued chunk {} => queue size {}", chunk.getId(), size);
        } catch (Exception e) {
            log.error("Failed to enqueue chunk {}", chunk.getId(), e);
            throw new IllegalStateException("Failed to enqueue", e);
        }
    }

    public LogChunk dequeue() {
        try {
            var result = jedis.brpop(1, QUEUE_KEY);
            if (result == null || result.size() < 2) {
                return null;
            }
            String json = result.get(1);
            return mapper.readValue(json, LogChunk.class);
        } catch (Exception e) {
            log.error("Failed to dequeue chunk", e);
            return null;
        }
    }
}
