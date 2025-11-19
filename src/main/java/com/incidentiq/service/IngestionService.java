package com.incidentiq.service;

import com.incidentiq.model.IngestionJob;
import com.incidentiq.model.LogChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates ingestion jobs and persists job state in Redis (HASH).
 */
@Service
public class IngestionService {

    private final LogChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final JedisPooled jedis;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, IngestionJob> jobs = new ConcurrentHashMap<>();
    private static final String JOB_KEY_PREFIX = "job:";

    public IngestionService(
            LogChunkService chunkService,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port
    ) {
        this.chunkService = chunkService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.jedis = new JedisPooled(host, port);
    }

    public IngestionJob createAndStartJobFromText(String source, String fileName, String text) {
        IngestionJob job = IngestionJob.newJob();
        jobs.put(job.getId(), job);
        persistJob(job);
        executor.submit(() -> runIngestion(job.getId(), source, fileName, text));
        return job;
    }

    private void persistJob(IngestionJob job) {
        String key = JOB_KEY_PREFIX + job.getId();
        Map<String, String> map = new HashMap<>();
        map.put("id", job.getId());
        map.put("status", job.getStatus());
        map.put("message", job.getMessage() == null ? "" : job.getMessage());
        map.put("createdAt", job.getCreatedAt() == null ? Instant.now().toString() : job.getCreatedAt().toString());
        map.put("processedChunks", String.valueOf(job.getProcessedChunks()));
        map.put("totalChunks", String.valueOf(job.getTotalChunks()));
        jedis.hset(key, map);
        // expire job metadata after 7 days
        jedis.expire(key, 60 * 60 * 24 * 7);
    }

    private void updateJobStatus(String jobId, String status, String message) {
        IngestionJob job = jobs.get(jobId);
        if (job == null) return;
        job.setStatus(status);
        job.setMessage(message);
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) job.setFinishedAt(Instant.now());
        persistJob(job);
    }

    private void runIngestion(String jobId, String source, String fileName, String text) {
        updateJobStatus(jobId, "RUNNING", null);
        try {
            List<LogChunk> chunks = chunkService.chunk(source, fileName, text);
            IngestionJob job = jobs.get(jobId);
            if (job != null) { job.setTotalChunks(chunks.size()); persistJob(job); }
            int processed = 0;
            for (int i = 0; i < chunks.size(); i++) {
                LogChunk c = chunks.get(i);
                float[] emb = embeddingService.getEmbedding(c.getText());
                Map<String,Object> meta = new HashMap<>();
                meta.put("jobId", jobId);
                meta.put("chunkNo", c.getChunkNo());
                meta.put("source", source);
                meta.put("fileName", fileName);
                meta.put("createdAt", Instant.now().toString());
                vectorStoreService.upsert(c.getId(), emb, c.getText(), meta);
                processed++;
                if (job != null) {
                    job.setProcessedChunks(processed);
                    if (processed % 10 == 0 || processed == chunks.size()) persistJob(job);
                }
            }
            updateJobStatus(jobId, "SUCCESS", "Stored " + processed + " chunks");
        } catch (Exception e) {
            updateJobStatus(jobId, "FAILED", e.getMessage());
        }
    }

    public IngestionJob getJob(String id) {
        IngestionJob j = jobs.get(id);
        if (j != null) return j;
        String key = JOB_KEY_PREFIX + id;
        if (!jedis.exists(key)) return null;
        Map<String, String> map = jedis.hgetAll(key);
        IngestionJob job = new IngestionJob();
        job.setId(map.getOrDefault("id", id));
        job.setStatus(map.getOrDefault("status", "UNKNOWN"));
        job.setMessage(map.getOrDefault("message", ""));
        job.setProcessedChunks(Integer.parseInt(map.getOrDefault("processedChunks", "0")));
        job.setTotalChunks(Integer.parseInt(map.getOrDefault("totalChunks", "0")));
        jobs.put(job.getId(), job);
        return job;
    }
}
