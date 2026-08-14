package com.civic.reporting.service.impl;

import com.civic.reporting.dto.response.DepartmentResponse;
import com.civic.reporting.entity.Department;
import com.civic.reporting.exception.ResourceNotFoundException;
import com.civic.reporting.repository.DepartmentRepository;
import com.civic.reporting.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(DepartmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        return DepartmentResponse.fromEntity(getDepartmentEntity(id));
    }

    @Override
    public DepartmentResponse getDepartmentByCode(String code) {
        Department dept = departmentRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with code: " + code));
        return DepartmentResponse.fromEntity(dept);
    }

    @Override
    public Department getDepartmentEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
    }
}
