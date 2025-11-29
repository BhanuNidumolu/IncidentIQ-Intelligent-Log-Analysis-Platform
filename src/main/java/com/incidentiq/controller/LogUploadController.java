package com.incidentiq.controller;

import com.incidentiq.model.LogChunk;
import com.incidentiq.service.IngestionService;
import com.incidentiq.service.LogChunkService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/logs")
public class LogUploadController {

    private final LogChunkService chunkService;
    private final IngestionService ingestionService;

    public LogUploadController(LogChunkService chunkService,
                               IngestionService ingestionService) {
        this.chunkService = chunkService;
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadFile(@RequestPart("file") MultipartFile file) throws Exception {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        var job = ingestionService.createAndStartJobFromText(
                "file",
                file.getOriginalFilename(),
                sb.toString()
        );

        return Map.of(
                "jobId", job.getId(),
                "status", job.getStatus(),
                "message", "Ingestion started for " + file.getOriginalFilename()
        );
    }

    @PostMapping(value = "/uploadTextAsync", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Map<String, String> uploadTextAsync(@RequestBody String text) {

        var job = ingestionService.createAndStartJobFromText(
                "inline",
                "text-block",
                text
        );

        return Map.of(
                "jobId", job.getId(),
                "status", job.getStatus()
        );
    }

    @PostMapping(value = "/uploadText", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String uploadTextSync(@RequestBody String text) {
        List<LogChunk> chunks = chunkService.chunk("text", "inline", text);
        return "Chunks prepared: " + chunks.size() +
                " (embedding & DB storage is only supported in async mode)";
    }
}
