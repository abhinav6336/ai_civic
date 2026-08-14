package com.civic.reporting.dto.response;

import java.util.Map;

public class DashboardStatsResponse {
    private long totalIssues;
    private long reportedIssues;
    private long aiClassifiedIssues;
    private long assignedIssues;
    private long inProgressIssues;
    private long resolvedIssues;
    private long rejectedIssues;
    
    private Map<String, Long> issuesByCategory;
    private Map<String, Long> issuesByDepartment;

    public DashboardStatsResponse() {
    }

    public long getTotalIssues() {
        return totalIssues;
    }

    public void setTotalIssues(long totalIssues) {
        this.totalIssues = totalIssues;
    }

    public long getReportedIssues() {
        return reportedIssues;
    }

    public void setReportedIssues(long reportedIssues) {
        this.reportedIssues = reportedIssues;
    }

    public long getAiClassifiedIssues() {
        return aiClassifiedIssues;
    }

    public void setAiClassifiedIssues(long aiClassifiedIssues) {
        this.aiClassifiedIssues = aiClassifiedIssues;
    }

    public long getAssignedIssues() {
        return assignedIssues;
    }

    public void setAssignedIssues(long assignedIssues) {
        this.assignedIssues = assignedIssues;
    }

    public long getInProgressIssues() {
        return inProgressIssues;
    }

    public void setInProgressIssues(long inProgressIssues) {
        this.inProgressIssues = inProgressIssues;
    }

    public long getResolvedIssues() {
        return resolvedIssues;
    }

    public void setResolvedIssues(long resolvedIssues) {
        this.resolvedIssues = resolvedIssues;
    }

    public long getRejectedIssues() {
        return rejectedIssues;
    }

    public void setRejectedIssues(long rejectedIssues) {
        this.rejectedIssues = rejectedIssues;
    }

    public Map<String, Long> getIssuesByCategory() {
        return issuesByCategory;
    }

    public void setIssuesByCategory(Map<String, Long> issuesByCategory) {
        this.issuesByCategory = issuesByCategory;
    }

    public Map<String, Long> getIssuesByDepartment() {
        return issuesByDepartment;
    }

    public void setIssuesByDepartment(Map<String, Long> issuesByDepartment) {
        this.issuesByDepartment = issuesByDepartment;
    }
}
