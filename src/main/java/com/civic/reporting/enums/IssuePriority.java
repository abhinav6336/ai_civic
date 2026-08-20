package com.civic.reporting.enums;

public enum IssuePriority {
    CRITICAL("Critical", "#dc2626", 4),     // 4-hour SLA
    HIGH("High", "#ea580c", 24),           // 24-hour SLA
    MEDIUM("Medium", "#2563eb", 72),       // 72-hour SLA
    LOW("Low", "#64748b", 168);            // 7-day SLA

    private final String label;
    private final String color;
    private final int defaultSlaHours;

    IssuePriority(String label, String color, int defaultSlaHours) {
        this.label = label;
        this.color = color;
        this.defaultSlaHours = defaultSlaHours;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    public int getDefaultSlaHours() {
        return defaultSlaHours;
    }
}
