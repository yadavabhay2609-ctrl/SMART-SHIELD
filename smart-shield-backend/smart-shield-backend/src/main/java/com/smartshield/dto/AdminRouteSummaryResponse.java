package com.smartshield.dto;

public class AdminRouteSummaryResponse {

    private long totalRoutes;
    private double averageSafetyScore;
    private long highRiskRouteCount;
    private long totalRatings;
    private long autoFiledGrievances;

    public AdminRouteSummaryResponse() {
    }

    public AdminRouteSummaryResponse(long totalRoutes, double averageSafetyScore,
                                    long highRiskRouteCount, long totalRatings,
                                    long autoFiledGrievances) {
        this.totalRoutes = totalRoutes;
        this.averageSafetyScore = averageSafetyScore;
        this.highRiskRouteCount = highRiskRouteCount;
        this.totalRatings = totalRatings;
        this.autoFiledGrievances = autoFiledGrievances;
    }

    public long getTotalRoutes() {
        return totalRoutes;
    }

    public void setTotalRoutes(long totalRoutes) {
        this.totalRoutes = totalRoutes;
    }

    public double getAverageSafetyScore() {
        return averageSafetyScore;
    }

    public void setAverageSafetyScore(double averageSafetyScore) {
        this.averageSafetyScore = averageSafetyScore;
    }

    public long getHighRiskRouteCount() {
        return highRiskRouteCount;
    }

    public void setHighRiskRouteCount(long highRiskRouteCount) {
        this.highRiskRouteCount = highRiskRouteCount;
    }

    public long getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(long totalRatings) {
        this.totalRatings = totalRatings;
    }

    public long getAutoFiledGrievances() {
        return autoFiledGrievances;
    }

    public void setAutoFiledGrievances(long autoFiledGrievances) {
        this.autoFiledGrievances = autoFiledGrievances;
    }
}
