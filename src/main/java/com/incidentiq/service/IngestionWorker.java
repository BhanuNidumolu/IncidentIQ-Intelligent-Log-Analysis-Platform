package com.incidentiq.service;

import com.incidentiq.model.LogChunk;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionWorker {

    private final LogChunkService chunkService;
    private final VectorStoreService vectorStoreService;
    private final IngestionService ingestionService;

    private final Thread workerThread = new Thread(this::runWorker, "ingestion-worker");

    @PostConstruct
    public void init() {
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("IngestionWorker started");
    }

    private void runWorker() {
        while (true) {
            try {
                LogChunk chunk = chunkService.dequeue();
                if (chunk == null) {
                    continue;
                }

                log.info("Worker received chunk {}", chunk.getId());

                vectorStoreService.storeEmbedding(chunk);
                ingestionService.incrementProcessedChunks(chunk.getJobId());
            } catch (Exception e) {
                log.error("Error in worker loop", e);
            }
        }
    }
}
