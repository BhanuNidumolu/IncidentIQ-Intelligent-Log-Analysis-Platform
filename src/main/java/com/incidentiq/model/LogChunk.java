package com.incidentiq.model;

import lombok.Data;

import java.util.UUID;

@Data
public class LogChunk {

    private String id;
    private String text;
    private String source;
    private String fileName;
    private int chunkNo;
    private String jobId;

    public LogChunk() {
        // Default constructor for Jackson
    }

    public LogChunk(String source, String fileName, int chunkNo, String text) {
        this.id = UUID.randomUUID().toString();
        this.source = source;
        this.fileName = fileName;
        this.chunkNo = chunkNo;
        this.text = text == null ? "" : text.trim();
    }
}
