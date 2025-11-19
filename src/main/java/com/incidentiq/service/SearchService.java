package com.incidentiq.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SearchService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final RediSearchKnnService rediSearchKnnService;

    public SearchService(EmbeddingService embeddingService,
                         VectorStoreService vectorStoreService,
                         RediSearchKnnService rediSearchKnnService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.rediSearchKnnService = rediSearchKnnService;
    }

    public List<VectorStoreService.SearchResult> search(String query, int k) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        float[] emb = embeddingService.getEmbedding(query);
        try {
            List<VectorStoreService.SearchResult> server = rediSearchKnnService.searchKnnServerSide(emb, k);
            if (server != null && !server.isEmpty()) return server;
        } catch (Exception e) {
            System.err.println("[SearchService] RediSearch failed: " + e.getMessage());
        }
        return vectorStoreService.searchKnn(emb, k);
    }
}
