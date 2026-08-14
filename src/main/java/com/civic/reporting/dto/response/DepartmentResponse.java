package com.civic.reporting.dto.response;

import com.civic.reporting.entity.Department;
import java.time.LocalDateTime;

public class DepartmentResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private boolean active;
    private LocalDateTime createdAt;

    public DepartmentResponse() {
    }

    public static DepartmentResponse fromEntity(Department dept) {
        if (dept == null) return null;
        DepartmentResponse res = new DepartmentResponse();
        res.setId(dept.getId());
        res.setCode(dept.getCode());
        res.setName(dept.getName());
        res.setDescription(dept.getDescription());
        res.setContactEmail(dept.getContactEmail());
        res.setContactPhone(dept.getContactPhone());
        res.setActive(dept.isActive());
        res.setCreatedAt(dept.getCreatedAt());
        return res;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
