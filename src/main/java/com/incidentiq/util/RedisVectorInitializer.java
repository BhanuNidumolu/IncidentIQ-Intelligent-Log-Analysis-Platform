package com.incidentiq.util;

import com.incidentiq.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class RedisVectorInitializer {

    private final JedisPooled jedis;
    private final EmbeddingService embeddingService;

    private final String indexName;
    private final String prefix;

    public RedisVectorInitializer(
            EmbeddingService embeddingService,
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${app.redis.index-name}") String indexName,
            @Value("${app.redis.prefix}") String prefix
    ) {
        this.jedis = new JedisPooled(host, port);
        this.embeddingService = embeddingService;
        this.indexName = indexName;
        this.prefix = prefix;

        createIndexIfNeeded();
    }

    private void createIndexIfNeeded() {
        try {
            try {
                jedis.ftInfo(indexName);
                System.out.println("[RedisVectorInitializer] Index exists: " + indexName);
                return;
            } catch (Exception ignored) {
                System.out.println("[RedisVectorInitializer] Index not found → creating: " + indexName);
            }

            int dim = embeddingService.getEmbeddingDim();

            List<byte[]> args = new ArrayList<>();
            args.add(indexName.getBytes(StandardCharsets.UTF_8));
            args.add("ON".getBytes());
            args.add("HASH".getBytes());
            args.add("PREFIX".getBytes());
            args.add("1".getBytes());
            args.add(prefix.getBytes());
            args.add("SCHEMA".getBytes());
            args.add("text".getBytes());
            args.add("TEXT".getBytes());
            args.add("vector".getBytes());
            args.add("VECTOR".getBytes());
            args.add("HNSW".getBytes());
            args.add("6".getBytes());
            args.add("TYPE".getBytes());
            args.add("FLOAT32".getBytes());
            args.add("DIM".getBytes());
            args.add(String.valueOf(dim).getBytes());
            args.add("DISTANCE_METRIC".getBytes());
            args.add("COSINE".getBytes());

            jedis.sendCommand(RedisSearchCommand.FT_CREATE, args.toArray(new byte[0][]));

            System.out.println("[RedisVectorInitializer] Created index '" + indexName + "' (DIM=" + dim + ")");
        }
        catch (Exception e) {
            System.err.println("[RedisVectorInitializer] Failed to create index: " + e.getMessage());
        }
    }
}
