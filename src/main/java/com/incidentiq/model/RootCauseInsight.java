package com.incidentiq.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RootCauseInsight {

    private String query;

    private String rootCauseSummary;
    private String probableRootCause;
    private String impact;
    private String recommendedActions;
    private String confidenceLevel;
    private String rawAnalysis;

    private List<EvidenceHit> evidence;

    @Data
    @Builder
    public static class EvidenceHit {
        private String id;
        private String text;
        private double score;
        private String meta;
    }
}
