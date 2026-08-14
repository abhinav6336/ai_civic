package com.civic.reporting.dto.response;

import com.civic.reporting.entity.Issue;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IssueResponse {
    private Long id;
    private String trackingNumber;
    private String title;
    private String description;
    private IssueCategory category;
    private String categoryDisplayName;
    private IssueStatus status;
    private String statusLabel;
    private Double latitude;
    private Double longitude;
    private String address;
    private String imageUrl;
    
    private Long citizenId;
    private String citizenName;
    private String citizenEmail;
    
    private Long assignedDepartmentId;
    private String assignedDepartmentName;
    private String assignedDepartmentCode;

    private Long assignedOfficerId;
    private String assignedOfficerName;

    private Double aiConfidence;
    private String aiSuggestedCategory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<IssueUpdateResponse> updates;

    public IssueResponse() {
    }

    public static IssueResponse fromEntity(Issue issue, boolean includeUpdates) {
        if (issue == null) return null;
        IssueResponse res = new IssueResponse();
        res.setId(issue.getId());
        res.setTrackingNumber(issue.getTrackingNumber());
        res.setTitle(issue.getTitle());
        res.setDescription(issue.getDescription());
        res.setCategory(issue.getCategory());
        res.setCategoryDisplayName(issue.getCategory() != null ? issue.getCategory().getDisplayName() : null);
        res.setStatus(issue.getStatus());
        res.setStatusLabel(issue.getStatus() != null ? issue.getStatus().getLabel() : null);
        res.setLatitude(issue.getLatitude());
        res.setLongitude(issue.getLongitude());
        res.setAddress(issue.getAddress());
        res.setImageUrl(issue.getImageUrl());

        if (issue.getCitizen() != null) {
            res.setCitizenId(issue.getCitizen().getId());
            res.setCitizenName(issue.getCitizen().getName());
            res.setCitizenEmail(issue.getCitizen().getEmail());
        }

        if (issue.getAssignedDepartment() != null) {
            res.setAssignedDepartmentId(issue.getAssignedDepartment().getId());
            res.setAssignedDepartmentName(issue.getAssignedDepartment().getName());
            res.setAssignedDepartmentCode(issue.getAssignedDepartment().getCode());
        }

        if (issue.getAssignedOfficer() != null) {
            res.setAssignedOfficerId(issue.getAssignedOfficer().getId());
            res.setAssignedOfficerName(issue.getAssignedOfficer().getName());
        }

        res.setAiConfidence(issue.getAiConfidence());
        res.setAiSuggestedCategory(issue.getAiSuggestedCategory());
        res.setCreatedAt(issue.getCreatedAt());
        res.setUpdatedAt(issue.getUpdatedAt());

        if (includeUpdates && issue.getUpdates() != null) {
            res.setUpdates(issue.getUpdates().stream()
                    .map(IssueUpdateResponse::fromEntity)
                    .collect(Collectors.toList()));
        } else {
            res.setUpdates(Collections.emptyList());
        }

        return res;
    }

    public static IssueResponse fromEntity(Issue issue) {
        return fromEntity(issue, false);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IssueCategory getCategory() {
        return category;
    }

    public void setCategory(IssueCategory category) {
        this.category = category;
    }

    public String getCategoryDisplayName() {
        return categoryDisplayName;
    }

    public void setCategoryDisplayName(String categoryDisplayName) {
        this.categoryDisplayName = categoryDisplayName;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(Long citizenId) {
        this.citizenId = citizenId;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public String getCitizenEmail() {
        return citizenEmail;
    }

    public void setCitizenEmail(String citizenEmail) {
        this.citizenEmail = citizenEmail;
    }

    public Long getAssignedDepartmentId() {
        return assignedDepartmentId;
    }

    public void setAssignedDepartmentId(Long assignedDepartmentId) {
        this.assignedDepartmentId = assignedDepartmentId;
    }

    public String getAssignedDepartmentName() {
        return assignedDepartmentName;
    }

    public void setAssignedDepartmentName(String assignedDepartmentName) {
        this.assignedDepartmentName = assignedDepartmentName;
    }

    public String getAssignedDepartmentCode() {
        return assignedDepartmentCode;
    }

    public void setAssignedDepartmentCode(String assignedDepartmentCode) {
        this.assignedDepartmentCode = assignedDepartmentCode;
    }

    public Long getAssignedOfficerId() {
        return assignedOfficerId;
    }

    public void setAssignedOfficerId(Long assignedOfficerId) {
        this.assignedOfficerId = assignedOfficerId;
    }

    public String getAssignedOfficerName() {
        return assignedOfficerName;
    }

    public void setAssignedOfficerName(String assignedOfficerName) {
        this.assignedOfficerName = assignedOfficerName;
    }

    public Double getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(Double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public String getAiSuggestedCategory() {
        return aiSuggestedCategory;
    }

    public void setAiSuggestedCategory(String aiSuggestedCategory) {
        this.aiSuggestedCategory = aiSuggestedCategory;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<IssueUpdateResponse> getUpdates() {
        return updates;
    }

    public void setUpdates(List<IssueUpdateResponse> updates) {
        this.updates = updates;
    }
}
