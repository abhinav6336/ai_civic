package com.civic.reporting.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CivicInsightsResponse {

    private ObservedData observedData;
    private AlgorithmicInsights algorithmicInsights;
    private LocalDateTime generatedAt;

    public CivicInsightsResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    public CivicInsightsResponse(ObservedData observedData, AlgorithmicInsights algorithmicInsights) {
        this.observedData = observedData;
        this.algorithmicInsights = algorithmicInsights;
        this.generatedAt = LocalDateTime.now();
    }

    // =========================================================================
    // 1. OBSERVED DATA (Pure Ground-Truth Historical Metrics)
    // =========================================================================
    public static class ObservedData {
        private long totalComplaints;
        private long pendingComplaints;
        private long inProgressComplaints;
        private long resolvedComplaints;
        private double overallResolutionRate; // in %
        private List<CategoryMetric> categoryBreakdown;
        private List<DepartmentWorkloadMetric> departmentWorkloads;
        private List<LocationFrequencyMetric> frequentLocations;
        private Map<String, Long> statusCounts;

        public ObservedData() {}

        public long getTotalComplaints() { return totalComplaints; }
        public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }
        public long getPendingComplaints() { return pendingComplaints; }
        public void setPendingComplaints(long pendingComplaints) { this.pendingComplaints = pendingComplaints; }
        public long getInProgressComplaints() { return inProgressComplaints; }
        public void setInProgressComplaints(long inProgressComplaints) { this.inProgressComplaints = inProgressComplaints; }
        public long getResolvedComplaints() { return resolvedComplaints; }
        public void setResolvedComplaints(long resolvedComplaints) { this.resolvedComplaints = resolvedComplaints; }
        public double getOverallResolutionRate() { return overallResolutionRate; }
        public void setOverallResolutionRate(double overallResolutionRate) { this.overallResolutionRate = overallResolutionRate; }
        public List<CategoryMetric> getCategoryBreakdown() { return categoryBreakdown; }
        public void setCategoryBreakdown(List<CategoryMetric> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
        public List<DepartmentWorkloadMetric> getDepartmentWorkloads() { return departmentWorkloads; }
        public void setDepartmentWorkloads(List<DepartmentWorkloadMetric> departmentWorkloads) { this.departmentWorkloads = departmentWorkloads; }
        public List<LocationFrequencyMetric> getFrequentLocations() { return frequentLocations; }
        public void setFrequentLocations(List<LocationFrequencyMetric> frequentLocations) { this.frequentLocations = frequentLocations; }
        public Map<String, Long> getStatusCounts() { return statusCounts; }
        public void setStatusCounts(Map<String, Long> statusCounts) { this.statusCounts = statusCounts; }
    }

    public static class CategoryMetric {
        private String category;
        private String displayName;
        private long count;
        private double percentage;

        public CategoryMetric() {}
        public CategoryMetric(String category, String displayName, long count, double percentage) {
            this.category = category;
            this.displayName = displayName;
            this.count = count;
            this.percentage = percentage;
        }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    public static class DepartmentWorkloadMetric {
        private Long departmentId;
        private String departmentName;
        private String code;
        private long pendingCount;
        private long inProgressCount;
        private long resolvedCount;
        private long totalCount;
        private double workloadSharePercent;
        private double resolutionRate;

        public DepartmentWorkloadMetric() {}
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
        public long getInProgressCount() { return inProgressCount; }
        public void setInProgressCount(long inProgressCount) { this.inProgressCount = inProgressCount; }
        public long getResolvedCount() { return resolvedCount; }
        public void setResolvedCount(long resolvedCount) { this.resolvedCount = resolvedCount; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public double getWorkloadSharePercent() { return workloadSharePercent; }
        public void setWorkloadSharePercent(double workloadSharePercent) { this.workloadSharePercent = workloadSharePercent; }
        public double getResolutionRate() { return resolutionRate; }
        public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }
    }

    public static class LocationFrequencyMetric {
        private String location;
        private long totalCount;
        private long pendingCount;
        private long resolvedCount;
        private String primaryCategory;

        public LocationFrequencyMetric() {}
        public LocationFrequencyMetric(String location, long totalCount, long pendingCount, long resolvedCount, String primaryCategory) {
            this.location = location;
            this.totalCount = totalCount;
            this.pendingCount = pendingCount;
            this.resolvedCount = resolvedCount;
            this.primaryCategory = primaryCategory;
        }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
        public long getResolvedCount() { return resolvedCount; }
        public void setResolvedCount(long resolvedCount) { this.resolvedCount = resolvedCount; }
        public String getPrimaryCategory() { return primaryCategory; }
        public void setPrimaryCategory(String primaryCategory) { this.primaryCategory = primaryCategory; }
    }

    // =========================================================================
    // 2. AI / ALGORITHMIC INSIGHTS (Derived Analytics & Clustering)
    // =========================================================================
    public static class AlgorithmicInsights {
        private List<SpatialCluster> spatialClusters;
        private List<RecurringIssuePattern> recurringPatterns;
        private List<WorkloadBottleneck> workloadBottlenecks;
        private List<UnresolvedComplaintPattern> unresolvedPatterns;
        private List<ActionableRecommendation> recommendations;

        public AlgorithmicInsights() {}

        public List<SpatialCluster> getSpatialClusters() { return spatialClusters; }
        public void setSpatialClusters(List<SpatialCluster> spatialClusters) { this.spatialClusters = spatialClusters; }
        public List<RecurringIssuePattern> getRecurringPatterns() { return recurringPatterns; }
        public void setRecurringPatterns(List<RecurringIssuePattern> recurringPatterns) { this.recurringPatterns = recurringPatterns; }
        public List<WorkloadBottleneck> getWorkloadBottlenecks() { return workloadBottlenecks; }
        public void setWorkloadBottlenecks(List<WorkloadBottleneck> workloadBottlenecks) { this.workloadBottlenecks = workloadBottlenecks; }
        public List<UnresolvedComplaintPattern> getUnresolvedPatterns() { return unresolvedPatterns; }
        public void setUnresolvedPatterns(List<UnresolvedComplaintPattern> unresolvedPatterns) { this.unresolvedPatterns = unresolvedPatterns; }
        public List<ActionableRecommendation> getRecommendations() { return recommendations; }
        public void setRecommendations(List<ActionableRecommendation> recommendations) { this.recommendations = recommendations; }
    }

    public static class SpatialCluster {
        private String clusterId;
        private String clusterName;
        private double latitude;
        private double longitude;
        private int complaintCount;
        private String dominantCategory;
        private String riskLevel; // HIGH, MEDIUM, LOW
        private List<String> trackingNumbers;
        private String summary;

        public SpatialCluster() {}
        public String getClusterId() { return clusterId; }
        public void setClusterId(String clusterId) { this.clusterId = clusterId; }
        public String getClusterName() { return clusterName; }
        public void setClusterName(String clusterName) { this.clusterName = clusterName; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public int getComplaintCount() { return complaintCount; }
        public void setComplaintCount(int complaintCount) { this.complaintCount = complaintCount; }
        public String getDominantCategory() { return dominantCategory; }
        public void setDominantCategory(String dominantCategory) { this.dominantCategory = dominantCategory; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public List<String> getTrackingNumbers() { return trackingNumbers; }
        public void setTrackingNumbers(List<String> trackingNumbers) { this.trackingNumbers = trackingNumbers; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    public static class RecurringIssuePattern {
        private String location;
        private String category;
        private int occurrences;
        private String recurrenceType; // CHRONIC_SPOT, SEASONAL_DRAINAGE, FREQUENT_INFRASTRUCTURE
        private String severity; // HIGH, MEDIUM
        private String diagnosis;
        private String recommendation;

        public RecurringIssuePattern() {}
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getOccurrences() { return occurrences; }
        public void setOccurrences(int occurrences) { this.occurrences = occurrences; }
        public String getRecurrenceType() { return recurrenceType; }
        public void setRecurrenceType(String recurrenceType) { this.recurrenceType = recurrenceType; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getDiagnosis() { return diagnosis; }
        public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    public static class WorkloadBottleneck {
        private String departmentName;
        private String code;
        private long pendingBacklog;
        private String bottleneckSeverity; // CRITICAL, ELEVATED, NORMAL
        private double workloadPressureIndex;
        private String recommendation;

        public WorkloadBottleneck() {}
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public long getPendingBacklog() { return pendingBacklog; }
        public void setPendingBacklog(long pendingBacklog) { this.pendingBacklog = pendingBacklog; }
        public String getBottleneckSeverity() { return bottleneckSeverity; }
        public void setBottleneckSeverity(String bottleneckSeverity) { this.bottleneckSeverity = bottleneckSeverity; }
        public double getWorkloadPressureIndex() { return workloadPressureIndex; }
        public void setWorkloadPressureIndex(double workloadPressureIndex) { this.workloadPressureIndex = workloadPressureIndex; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    public static class UnresolvedComplaintPattern {
        private String title;
        private String patternKey;
        private int affectedCount;
        private String description;
        private String actionSuggested;

        public UnresolvedComplaintPattern() {}
        public UnresolvedComplaintPattern(String title, String patternKey, int affectedCount, String description, String actionSuggested) {
            this.title = title;
            this.patternKey = patternKey;
            this.affectedCount = affectedCount;
            this.description = description;
            this.actionSuggested = actionSuggested;
        }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getPatternKey() { return patternKey; }
        public void setPatternKey(String patternKey) { this.patternKey = patternKey; }
        public int getAffectedCount() { return affectedCount; }
        public void setAffectedCount(int affectedCount) { this.affectedCount = affectedCount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getActionSuggested() { return actionSuggested; }
        public void setActionSuggested(String actionSuggested) { this.actionSuggested = actionSuggested; }
    }

    public static class ActionableRecommendation {
        private String id;
        private String title;
        private String category;
        private String priority; // URGENT, HIGH, MEDIUM
        private String rationale;
        private String recommendedAction;

        public ActionableRecommendation() {}
        public ActionableRecommendation(String id, String title, String category, String priority, String rationale, String recommendedAction) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.priority = priority;
            this.rationale = rationale;
            this.recommendedAction = recommendedAction;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getRationale() { return rationale; }
        public void setRationale(String rationale) { this.rationale = rationale; }
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    }

    public ObservedData getObservedData() { return observedData; }
    public void setObservedData(ObservedData observedData) { this.observedData = observedData; }
    public AlgorithmicInsights getAlgorithmicInsights() { return algorithmicInsights; }
    public void setAlgorithmicInsights(AlgorithmicInsights algorithmicInsights) { this.algorithmicInsights = algorithmicInsights; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
