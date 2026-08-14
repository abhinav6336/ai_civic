package com.civic.reporting.dto.response;

import com.civic.reporting.entity.IssueUpdate;
import com.civic.reporting.enums.IssueStatus;
import java.time.LocalDateTime;

public class IssueUpdateResponse {
    private Long id;
    private IssueStatus previousStatus;
    private IssueStatus newStatus;
    private String updateType;
    private String notes;
    private Long updatedByUserId;
    private String updatedByUserName;
    private LocalDateTime createdAt;

    public IssueUpdateResponse() {
    }

    public static IssueUpdateResponse fromEntity(IssueUpdate update) {
        if (update == null) return null;
        IssueUpdateResponse res = new IssueUpdateResponse();
        res.setId(update.getId());
        res.setPreviousStatus(update.getPreviousStatus());
        res.setNewStatus(update.getNewStatus());
        res.setUpdateType(update.getUpdateType());
        res.setNotes(update.getNotes());
        if (update.getUpdatedByUser() != null) {
            res.setUpdatedByUserId(update.getUpdatedByUser().getId());
            res.setUpdatedByUserName(update.getUpdatedByUser().getName());
        } else {
            res.setUpdatedByUserName("System / AI Engine");
        }
        res.setCreatedAt(update.getCreatedAt());
        return res;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IssueStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(IssueStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public IssueStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(IssueStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public String getUpdatedByUserName() {
        return updatedByUserName;
    }

    public void setUpdatedByUserName(String updatedByUserName) {
        this.updatedByUserName = updatedByUserName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
