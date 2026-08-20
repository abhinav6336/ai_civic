package com.civic.reporting.dto.request;

import com.civic.reporting.enums.IssueCategory;

public class AiAnalysisRequest {
    private String title;
    private String description;
    private IssueCategory category;
    private Double latitude;
    private Double longitude;
    private String address;

    public AiAnalysisRequest() {
    }

    public AiAnalysisRequest(String title, String description, IssueCategory category, Double latitude, Double longitude, String address) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
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
}
