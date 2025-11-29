package com.incidentiq.controller;

import com.incidentiq.model.RootCauseInsight;
import com.incidentiq.service.IncidentInsightService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/insights")
@RequiredArgsConstructor
public class InsightController {

    private final IncidentInsightService incidentInsightService;

    @GetMapping("/ping")
    public String ping() {
        return "IncidentIQ Insights API is up";
    }

    @PostMapping("/root-cause")
    public RootCauseInsight getRootCause(@RequestBody RootCauseRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        return incidentInsightService.analyzeRootCause(request.getQuery(), topK);
    }

    @Data
    public static class RootCauseRequest {
        private String query;
        private Integer topK;
    }
}
