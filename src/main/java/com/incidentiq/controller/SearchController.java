package com.incidentiq.controller;

import com.incidentiq.service.SearchService;
import com.incidentiq.service.VectorStoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public List<VectorStoreService.SearchResult> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int k) {
        return searchService.search(query, k);
    }
}
