package com.civic.reporting.entity;

import com.civic.reporting.enums.IssueStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "issue_updates", indexes = {
    @Index(name = "idx_issue_updates_issue_id", columnList = "issue_id")
})
public class IssueUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private IssueStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private IssueStatus newStatus;

    @Column(name = "update_type", nullable = false, length = 60)
    private String updateType; // INITIAL_REPORT, AI_CLASSIFICATION, STATUS_CHANGE, OFFICER_ASSIGNMENT, RESOLUTION_NOTE

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IssueUpdate() {
    }

    public IssueUpdate(Issue issue, User updatedByUser, IssueStatus previousStatus, IssueStatus newStatus, String updateType, String notes) {
        this.issue = issue;
        this.updatedByUser = updatedByUser;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.updateType = updateType;
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public User getUpdatedByUser() {
        return updatedByUser;
    }

    public void setUpdatedByUser(User updatedByUser) {
        this.updatedByUser = updatedByUser;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
