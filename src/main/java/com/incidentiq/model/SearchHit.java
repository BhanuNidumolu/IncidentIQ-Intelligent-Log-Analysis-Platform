package com.incidentiq.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchHit {
    private String id;
    private String text;
    private double score;
    private String meta;
}
