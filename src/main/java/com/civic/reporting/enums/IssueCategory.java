package com.civic.reporting.enums;

/**
 * Supported Civic Issue Categories.
 */
public enum IssueCategory {
    ROADS("Roads & Pavements", "Potholes, broken tarmac, missing road signs, road cave-ins"),
    ELECTRICITY("Electricity & Power", "Broken streetlights, dangling wires, transformer sparks, power outage"),
    GARBAGE_SANITATION("Garbage & Sanitation", "Overflowing bins, uncollected waste, illegal dumping, public litter"),
    WATER("Water Supply", "Pipeline burst, contaminated water, low pressure, water supply disruption"),
    DRAINAGE("Drainage & Sewage", "Clogged drains, open manholes, sewage overflow, waterlogging"),
    OTHER("Other Civic Issues", "General municipal and civic infrastructure concerns");

    private final String displayName;
    private final String description;

    IssueCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
