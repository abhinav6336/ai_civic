package com.civic.reporting.dto.response;

import com.civic.reporting.entity.User;
import com.civic.reporting.enums.UserRole;

public class AuthResponse {
    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private Long departmentId;
    private String departmentName;
    private String token;

    public AuthResponse() {
    }

    public static AuthResponse fromUser(User user, String token) {
        AuthResponse res = new AuthResponse();
        res.setId(user.getId());
        res.setName(user.getName());
        res.setEmail(user.getEmail());
        res.setRole(user.getRole());
        if (user.getDepartment() != null) {
            res.setDepartmentId(user.getDepartment().getId());
            res.setDepartmentName(user.getDepartment().getName());
        }
        res.setToken(token);
        return res;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
