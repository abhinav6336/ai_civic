package com.civic.reporting.dto.request;

import com.civic.reporting.enums.IssueStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IssueStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private IssueStatus newStatus;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    private Long updatedByUserId;

    public IssueStatusUpdateRequest() {
    }

    public IssueStatusUpdateRequest(IssueStatus newStatus, String notes, Long updatedByUserId) {
        this.newStatus = newStatus;
        this.notes = notes;
        this.updatedByUserId = updatedByUserId;
    }

    public IssueStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(IssueStatus newStatus) {
        this.newStatus = newStatus;
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
}
