package com.civic.reporting.ai;

import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssuePriority;

import java.util.Map;

public class AiAnalysisResult {
    private IssueCategory predictedCategory;
    private double confidenceScore;
    private IssuePriority priority;
    private int urgencyScore; // 1 to 100
    private int estimatedResolutionHours;
    private boolean isDuplicate;
    private String duplicateOfTrackingNumber;
    private double duplicateSimilarity;
    private String modelVersion;
    private long inferenceTimeMs;
    private String rationale;
    private Map<String, Double> categoryProbabilities;

    public AiAnalysisResult() {
    }

    public IssueCategory getPredictedCategory() {
        return predictedCategory;
    }

    public void setPredictedCategory(IssueCategory predictedCategory) {
        this.predictedCategory = predictedCategory;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public void setPriority(IssuePriority priority) {
        this.priority = priority;
    }

    public int getUrgencyScore() {
        return urgencyScore;
    }

    public void setUrgencyScore(int urgencyScore) {
        this.urgencyScore = urgencyScore;
    }

    public int getEstimatedResolutionHours() {
        return estimatedResolutionHours;
    }

    public void setEstimatedResolutionHours(int estimatedResolutionHours) {
        this.estimatedResolutionHours = estimatedResolutionHours;
    }

    public boolean isDuplicate() {
        return isDuplicate;
    }

    public void setDuplicate(boolean duplicate) {
        isDuplicate = duplicate;
    }

    public String getDuplicateOfTrackingNumber() {
        return duplicateOfTrackingNumber;
    }

    public void setDuplicateOfTrackingNumber(String duplicateOfTrackingNumber) {
        this.duplicateOfTrackingNumber = duplicateOfTrackingNumber;
    }

    public double getDuplicateSimilarity() {
        return duplicateSimilarity;
    }

    public void setDuplicateSimilarity(double duplicateSimilarity) {
        this.duplicateSimilarity = duplicateSimilarity;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public long getInferenceTimeMs() {
        return inferenceTimeMs;
    }

    public void setInferenceTimeMs(long inferenceTimeMs) {
        this.inferenceTimeMs = inferenceTimeMs;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Map<String, Double> getCategoryProbabilities() {
        return categoryProbabilities;
    }

    public void setCategoryProbabilities(Map<String, Double> categoryProbabilities) {
        this.categoryProbabilities = categoryProbabilities;
    }
}
