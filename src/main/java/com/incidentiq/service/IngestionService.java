package com.incidentiq.service;

import com.incidentiq.model.IngestionJob;
import com.incidentiq.model.LogChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final LogChunkService chunkService;
    private final JedisPooled jedis;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, IngestionJob> jobs = new ConcurrentHashMap<>();

    private static final String JOB_KEY_PREFIX = "job:";

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
        map.put("finishedAt", job.getFinishedAt() == null ? "" : job.getFinishedAt().toString());
        map.put("processedChunks", String.valueOf(job.getProcessedChunks()));
        map.put("totalChunks", String.valueOf(job.getTotalChunks()));
        jedis.hset(key, map);
        jedis.expire(key, 60 * 60 * 24 * 7);
    }

    private void updateJobStatus(String jobId, String status, String message) {
        IngestionJob job = jobs.get(jobId);
        if (job == null) return;

        job.setStatus(status);
        job.setMessage(message);
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            job.setFinishedAt(Instant.now());
        }
        persistJob(job);
    }

    private void runIngestion(String jobId, String source, String fileName, String text) {
        updateJobStatus(jobId, "RUNNING", null);
        try {
            List<LogChunk> chunks = chunkService.chunk(source, fileName, text);
            IngestionJob job = jobs.get(jobId);
            if (job != null) {
                job.setTotalChunks(chunks.size());
                persistJob(job);
            }

            for (LogChunk c : chunks) {
                c.setJobId(jobId);
                chunkService.enqueue(c);
            }

        } catch (Exception e) {
            log.error("Ingestion failed for job {}", jobId, e);
            updateJobStatus(jobId, "FAILED", e.getMessage());
        }
    }

    public void incrementProcessedChunks(String jobId) {
        IngestionJob job = jobs.get(jobId);
        if (job == null) {
            job = getJob(jobId);
            if (job == null) return;
        }

        job.setProcessedChunks(job.getProcessedChunks() + 1);

        if (job.getTotalChunks() > 0 &&
                job.getProcessedChunks() >= job.getTotalChunks() &&
                !"FAILED".equals(job.getStatus())) {

            job.setStatus("SUCCESS");
            job.setFinishedAt(Instant.now());
        }

        jobs.put(job.getId(), job);
        persistJob(job);
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
