package com.civic.reporting.enums;

/**
 * User roles in the platform.
 */
public enum UserRole {
    CITIZEN("Citizen", "Submits and tracks civic complaints"),
    OFFICER("Field Officer / Engineer", "Assigned issues to investigate and resolve"),
    ADMIN("Municipal Administrator", "Supervises departments, oversees issues, manages routing");

    private final String title;
    private final String description;

    UserRole(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
