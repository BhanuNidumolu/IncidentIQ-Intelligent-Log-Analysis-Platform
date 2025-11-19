package com.incidentiq.model;

import lombok.Data;

@Data
public class LogChunk {

    private String id;
    private String text;
    private String source;
    private String fileName;
    private int chunkNo;
}
