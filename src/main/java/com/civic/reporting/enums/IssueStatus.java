package com.civic.reporting.enums;

/**
 * Lifecycle states of a reported civic issue.
 */
public enum IssueStatus {
    REPORTED("Reported", "Issue successfully submitted by citizen, awaiting classification or review"),
    AI_CLASSIFIED("AI Classified", "Processed by Computer Vision AI service and routed to department"),
    ASSIGNED("Assigned", "Assigned to a specific municipal department or officer"),
    IN_PROGRESS("In Progress", "Work crew or officer has initiated resolution on site"),
    RESOLVED("Resolved", "Issue has been inspected, fixed, and verified"),
    REJECTED("Rejected", "Issue could not be validated or falls outside municipal jurisdiction");

    private final String label;
    private final String description;

    IssueStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
