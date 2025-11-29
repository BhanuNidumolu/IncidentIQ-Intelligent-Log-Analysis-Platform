package com.incidentiq.service;

import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Service
public class EmbeddingService {

    private final GeminiEmbeddingClient embeddingClient;

    public EmbeddingService(GeminiEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public float[] getEmbedding(String text) {
        return embeddingClient.embed(text == null ? "" : text);
    }

    public byte[] getEmbeddingAsBytes(String text) {
        float[] vec = getEmbedding(text);
        ByteBuffer buffer = ByteBuffer.allocate(vec.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (float f : vec) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    public int getEmbeddingDim() {
        return embeddingClient.getConfiguredDim();
    }

    public byte[] embedToBinary(String text) {
        return getEmbeddingAsBytes(text);
    }
}
