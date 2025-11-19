package com.incidentiq.service;

import com.incidentiq.model.LogChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Simple chunker: splits by newline and groups lines to create
 * ~500-900 char chunks, suitable for embedding.
 */
@Service
public class LogChunkService {

    // target chunk size
    private static final int MAX_CHUNK_SIZE = 900;
    private static final int MIN_CHUNK_SIZE = 300;

    public List<LogChunk> chunk(String source, String fileName, String text) {
        List<LogChunk> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;

        String[] lines = text.split("\\r?\\n");
        StringBuilder current = new StringBuilder();

        int chunkNo = 0;

        for (String line : lines) {

            // If adding this line makes chunk large → flush
            if (current.length() >= MIN_CHUNK_SIZE &&
                    current.length() + line.length() > MAX_CHUNK_SIZE) {

                out.add(makeChunk(current.toString(), source, fileName, chunkNo++));
                current = new StringBuilder();
            }

            current.append(line).append("\n");
        }

        // leftover
        if (current.length() > 0) {
            out.add(makeChunk(current.toString(), source, fileName, chunkNo));
        }

        return out;
    }

    private LogChunk makeChunk(String txt, String source, String fileName, int no) {
        LogChunk c = new LogChunk();
        c.setId(UUID.randomUUID().toString());
        c.setText(txt);
        c.setSource(source);
        c.setFileName(fileName);
        c.setChunkNo(no);
        return c;
    }
}
