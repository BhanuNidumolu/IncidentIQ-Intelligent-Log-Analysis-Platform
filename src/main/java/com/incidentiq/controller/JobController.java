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

    /**
     * POST /jobs/ingestText
     * Body: plain text
     * Optional: fileName (defaults to "inline")
     */
    @PostMapping(value = "/ingestText", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Map<String, Object> ingestText(@RequestBody String text,
                                          @RequestParam(defaultValue = "inline") String fileName) {

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

    /**
     * GET /jobs/{jobId}
     */
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
                "message", job.getMessage()
        );
    }
}
