package com.smartshield.dto;

public class SafetyFactorResponse {

    private String factorName;
    private String impact; // POSITIVE, NEUTRAL, NEGATIVE
    private Integer scoreContribution; // e.g. +10, -15
    private String description;

    public SafetyFactorResponse() {
    }

    public SafetyFactorResponse(String factorName, String impact, Integer scoreContribution, String description) {
        this.factorName = factorName;
        this.impact = impact;
        this.scoreContribution = scoreContribution;
        this.description = description;
    }

    public String getFactorName() {
        return factorName;
    }

    public void setFactorName(String factorName) {
        this.factorName = factorName;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public Integer getScoreContribution() {
        return scoreContribution;
    }

    public void setScoreContribution(Integer scoreContribution) {
        this.scoreContribution = scoreContribution;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
