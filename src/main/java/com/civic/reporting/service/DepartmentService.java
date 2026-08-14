package com.civic.reporting.service;

import com.civic.reporting.dto.response.DepartmentResponse;
import com.civic.reporting.entity.Department;

import java.util.List;

public interface DepartmentService {
    List<DepartmentResponse> getAllDepartments();
    DepartmentResponse getDepartmentById(Long id);
    DepartmentResponse getDepartmentByCode(String code);
    Department getDepartmentEntity(Long id);
}
