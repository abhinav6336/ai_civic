package com.civic.reporting.dto.response;

import com.civic.reporting.ai.AiAnalysisResult;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssuePriority;

import java.util.Map;

public class AiAnalysisResponse {
    private IssueCategory predictedCategory;
    private String predictedCategoryDisplayName;
    private double confidenceScore;
    private Map<String, Double> categoryProbabilities;
    private IssuePriority priority;
    private String priorityLabel;
    private String priorityColor;
    private int urgencyScore;
    private int estimatedResolutionHours;
    private boolean isDuplicate;
    private String duplicateOfTrackingNumber;
    private double duplicateSimilarity;
    private String rationale;
    private long inferenceTimeMs;
    private String modelVersion;

    public AiAnalysisResponse() {
    }

    public static AiAnalysisResponse fromResult(AiAnalysisResult result) {
        if (result == null) return null;
        AiAnalysisResponse res = new AiAnalysisResponse();
        res.setPredictedCategory(result.getPredictedCategory());
        res.setPredictedCategoryDisplayName(result.getPredictedCategory() != null ? result.getPredictedCategory().getDisplayName() : null);
        res.setConfidenceScore(result.getConfidenceScore());
        res.setCategoryProbabilities(result.getCategoryProbabilities());
        res.setPriority(result.getPriority());
        res.setPriorityLabel(result.getPriority() != null ? result.getPriority().getLabel() : null);
        res.setPriorityColor(result.getPriority() != null ? result.getPriority().getColor() : null);
        res.setUrgencyScore(result.getUrgencyScore());
        res.setEstimatedResolutionHours(result.getEstimatedResolutionHours());
        res.setDuplicate(result.isDuplicate());
        res.setDuplicateOfTrackingNumber(result.getDuplicateOfTrackingNumber());
        res.setDuplicateSimilarity(result.getDuplicateSimilarity());
        res.setRationale(result.getRationale());
        res.setInferenceTimeMs(result.getInferenceTimeMs());
        res.setModelVersion(result.getModelVersion());
        return res;
    }

    public IssueCategory getPredictedCategory() {
        return predictedCategory;
    }

    public void setPredictedCategory(IssueCategory predictedCategory) {
        this.predictedCategory = predictedCategory;
    }

    public String getPredictedCategoryDisplayName() {
        return predictedCategoryDisplayName;
    }

    public void setPredictedCategoryDisplayName(String predictedCategoryDisplayName) {
        this.predictedCategoryDisplayName = predictedCategoryDisplayName;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Map<String, Double> getCategoryProbabilities() {
        return categoryProbabilities;
    }

    public void setCategoryProbabilities(Map<String, Double> categoryProbabilities) {
        this.categoryProbabilities = categoryProbabilities;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public void setPriority(IssuePriority priority) {
        this.priority = priority;
    }

    public String getPriorityLabel() {
        return priorityLabel;
    }

    public void setPriorityLabel(String priorityLabel) {
        this.priorityLabel = priorityLabel;
    }

    public String getPriorityColor() {
        return priorityColor;
    }

    public void setPriorityColor(String priorityColor) {
        this.priorityColor = priorityColor;
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

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public long getInferenceTimeMs() {
        return inferenceTimeMs;
    }

    public void setInferenceTimeMs(long inferenceTimeMs) {
        this.inferenceTimeMs = inferenceTimeMs;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }
}
