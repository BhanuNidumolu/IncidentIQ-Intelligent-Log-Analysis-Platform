package com.incidentiq.controller;

import com.incidentiq.model.SearchHit;
import com.incidentiq.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/semantic")
    public List<SearchHit> semantic(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int k
    ) {
        return searchService.semanticSearch(query, k);
    }

    @GetMapping("/hybrid")
    public List<SearchHit> hybrid(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int k
    ) {
        return searchService.hybridSearch(query, k);
    }
}
