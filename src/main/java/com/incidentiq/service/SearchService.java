package com.incidentiq.service;

import com.incidentiq.model.SearchHit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final RediSearchKnnService redi;

    public List<SearchHit> semanticSearch(String query, int topK) {
        return redi.knnSearch(query, topK);
    }

    public List<SearchHit> hybridSearch(String query, int topK) {

        String terms = Arrays.stream(query.split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.joining("|"));

        if (terms.isEmpty()) {
            return semanticSearch(query, topK);
        }

        String filter = "@text:(" + terms + ")";

        return redi.hybridKnnSearch(query, filter, topK);
    }
}
