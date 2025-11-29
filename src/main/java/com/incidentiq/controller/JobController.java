package com.incidentiq.controller;

import com.incidentiq.model.IngestionJob;
import com.incidentiq.service.IngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final IngestionService ingestionService;

    public JobController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = "/ingestText", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Map<String, Object> ingestText(
            @RequestBody String text,
            @RequestParam(name = "fileName", defaultValue = "inline") String fileName
    ) {

        IngestionJob job = ingestionService.createAndStartJobFromText(
                "text",
                fileName,
                text
        );

        return Map.of(
                "jobId", job.getId(),
                "status", job.getStatus(),
                "message", "Ingestion started for text input"
        );
    }

    @GetMapping("/{jobId}")
    public Map<String, Object> getJob(@PathVariable String jobId) {
        IngestionJob job = ingestionService.getJob(jobId);

        if (job == null) {
            return Map.of(
                    "jobId", jobId,
                    "status", "NOT_FOUND",
                    "message", "No ingestion job exists with this ID"
            );
        }

        return Map.of(
                "jobId", job.getId(),
                "status", job.getStatus(),
                "processedChunks", job.getProcessedChunks(),
                "totalChunks", job.getTotalChunks(),
                "message", job.getMessage() == null ? "" : job.getMessage()
        );
    }
}
