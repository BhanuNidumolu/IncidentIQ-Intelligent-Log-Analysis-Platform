package com.incidentiq.service;

import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final GeminiEmbeddingClient embeddingClient;

    public EmbeddingService(GeminiEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public float[] getEmbedding(String text) {
        return embeddingClient.embed(text);
    }

    public byte[] getEmbeddingAsBytes(String text) {
        float[] v = getEmbedding(text);
        return embeddingClient.toFloat32Bytes(v);
    }

    public int getEmbeddingDim() {
        return embeddingClient.getConfiguredDim();
    }

    // convenience for chat, if you had embedding-based chat: keep embedding and chat separated.
}
