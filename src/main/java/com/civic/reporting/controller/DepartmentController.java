package com.civic.reporting.controller;

import com.civic.reporting.dto.response.ApiResponse;
import com.civic.reporting.dto.response.DepartmentResponse;
import com.civic.reporting.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments() {
        List<DepartmentResponse> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully", departments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(@PathVariable Long id) {
        DepartmentResponse dept = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.ok(dept));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentByCode(@PathVariable String code) {
        DepartmentResponse dept = departmentService.getDepartmentByCode(code);
        return ResponseEntity.ok(ApiResponse.ok(dept));
    }
}
