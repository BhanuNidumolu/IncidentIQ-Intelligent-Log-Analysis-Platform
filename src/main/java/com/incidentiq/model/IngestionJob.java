package com.incidentiq.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class IngestionJob {

    private String id;
    private String status; // PENDING, RUNNING, SUCCESS, FAILED
    private String message;

    private Instant createdAt;
    private Instant finishedAt;

    private int processedChunks;
    private int totalChunks;

    public static IngestionJob newJob() {
        IngestionJob j = new IngestionJob();
        j.id = UUID.randomUUID().toString();
        j.status = "PENDING";
        j.createdAt = Instant.now();
        j.processedChunks = 0;
        j.totalChunks = 0;
        return j;
    }
}
