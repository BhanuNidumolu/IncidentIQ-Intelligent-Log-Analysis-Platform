package com.incidentiq.service;

import com.incidentiq.util.RedisSearchCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@Service
public class RediSearchKnnService {

    private final JedisPooled jedis;
    private final String indexName;
    private final String prefix;

    public RediSearchKnnService(
            @Value("${spring.redis.host:localhost}") String host,
            @Value("${spring.redis.port:6379}") int port,
            @Value("${app.redis.index-name:idx:logs}") String indexName,
            @Value("${app.redis.prefix:log:}") String prefix
    ) {
        this.jedis = new JedisPooled(host, port);
        this.indexName = indexName;
        this.prefix = prefix;
    }

    /** Convert floats → Redis FLOAT32 little-endian vector */
    private static byte[] floatsToBytes(float[] vec) {
        ByteBuffer bb = ByteBuffer.allocate(vec.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : vec) bb.putFloat(f);
        return bb.array();
    }

    /**
     * Perform server-side KNN search using FT.SEARCH.
     * FALLBACK handled by SearchService.
     */
    public List<VectorStoreService.SearchResult> searchKnnServerSide(float[] queryEmb, int k) {
        try {
            // Convert to float32 bytes
            byte[] blob = floatsToBytes(queryEmb);

            // ---- FT.SEARCH Build ----
            // Query: "@vector=>[KNN k $blob AS dist]"
            String knnQuery = "*=>[KNN " + k + " @vector $blob AS dist]";

            List<byte[]> args = new ArrayList<>();
            args.add(indexName.getBytes());
            args.add(knnQuery.getBytes());

            // PARAMS 2 blob <bytes>
            args.add("PARAMS".getBytes());
            args.add("2".getBytes());
            args.add("blob".getBytes());
            args.add(blob);

            // SORTBY dist
            args.add("SORTBY".getBytes());
            args.add("dist".getBytes());

            // RETURN 2 text dist
            args.add("RETURN".getBytes());
            args.add("2".getBytes());
            args.add("text".getBytes());
            args.add("dist".getBytes());

            // Important: DIALECT 2
            args.add("DIALECT".getBytes());
            args.add("2".getBytes());

            byte[][] finalArgs = args.toArray(new byte[0][]);

            Object raw = jedis.sendCommand(RedisSearchCommand.FT_SEARCH, finalArgs);

            return parseResults(raw);

        } catch (Exception e) {
            System.err.println("[RediSearchKnnService] server-side search failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse Redis FT.SEARCH response
     * [ total, key1, fields1, key2, fields2, ... ]
     */
    private List<VectorStoreService.SearchResult> parseResults(Object reply) {
        List<VectorStoreService.SearchResult> out = new ArrayList<>();

        if (!(reply instanceof List<?> outer) || outer.size() < 2)
            return out;

        int index = 1;

        while (index < outer.size()) {

            // Document ID
            Object idObj = outer.get(index++);
            String id = idObj instanceof byte[] ? new String((byte[]) idObj) : idObj.toString();

            // Fields list
            Object fieldsObj = outer.get(index++);

            String text = "";
            double score = 0.0;

            if (fieldsObj instanceof List<?> fields) {
                for (int i = 0; i + 1 < fields.size(); i += 2) {
                    String key = new String((byte[]) fields.get(i));
                    byte[] valRaw = (byte[]) fields.get(i + 1);
                    String val = new String(valRaw);

                    if (key.equals("text"))
                        text = val;
                    if (key.equals("dist")) {
                        try { score = Double.parseDouble(val); } catch (Exception ignore) {}
                    }
                }
            }

            out.add(new VectorStoreService.SearchResult(id, text, score));
        }

        return out;
    }
}
