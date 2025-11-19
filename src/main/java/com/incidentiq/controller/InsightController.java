package com.incidentiq.controller;

import com.incidentiq.service.IncidentInsightService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/insight")
public class InsightController {

    private final IncidentInsightService insightService;

    public InsightController(IncidentInsightService insightService) {
        this.insightService = insightService;
    }

    @PostMapping
    public Object analyze(@RequestBody Map<String, String> body) throws Exception {
        String query = body.get("query");
        return insightService.generateInsight(query);
    }
}
