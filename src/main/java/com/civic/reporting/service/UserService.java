package com.civic.reporting.service;

import com.civic.reporting.dto.request.UserCreateRequest;
import com.civic.reporting.dto.response.UserResponse;
import com.civic.reporting.entity.User;
import com.civic.reporting.enums.UserRole;

import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse createUser(UserCreateRequest request);
    User getOrCreateCitizen(Long citizenId, String name, String email, String phone);
    User getUserEntity(Long id);
    List<UserResponse> getUsersByRole(UserRole role);
    List<UserResponse> getOfficersByDepartment(Long departmentId);
}
