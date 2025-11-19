package com.incidentiq.controller;

import com.incidentiq.service.EmbeddingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/embedding")
public class EmbeddingTestController {

    private final EmbeddingService embeddingService;

    public EmbeddingTestController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @GetMapping
    public float[] embedding(@RequestParam String text) {
        return embeddingService.getEmbedding(text);
    }
}
