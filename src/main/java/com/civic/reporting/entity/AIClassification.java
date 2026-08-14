package com.civic.reporting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_classifications", indexes = {
    @Index(name = "idx_ai_classifications_issue_id", columnList = "issue_id")
})
public class AIClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "predicted_category", nullable = false, length = 60)
    private String predictedCategory;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "raw_model_output", columnDefinition = "TEXT")
    private String rawModelOutput;

    @Column(name = "inference_time_ms")
    private Long inferenceTimeMs;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public AIClassification() {
    }

    public AIClassification(Issue issue, String predictedCategory, Double confidenceScore, String modelVersion, String rawModelOutput, Long inferenceTimeMs) {
        this.issue = issue;
        this.predictedCategory = predictedCategory;
        this.confidenceScore = confidenceScore;
        this.modelVersion = modelVersion;
        this.rawModelOutput = rawModelOutput;
        this.inferenceTimeMs = inferenceTimeMs;
    }

    @PrePersist
    protected void onCreate() {
        this.processedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Issue getIssue() {
        return issue;
    }

    public void setIssue(Issue issue) {
        this.issue = issue;
    }

    public String getPredictedCategory() {
        return predictedCategory;
    }

    public void setPredictedCategory(String predictedCategory) {
        this.predictedCategory = predictedCategory;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getRawModelOutput() {
        return rawModelOutput;
    }

    public void setRawModelOutput(String rawModelOutput) {
        this.rawModelOutput = rawModelOutput;
    }

    public Long getInferenceTimeMs() {
        return inferenceTimeMs;
    }

    public void setInferenceTimeMs(Long inferenceTimeMs) {
        this.inferenceTimeMs = inferenceTimeMs;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
