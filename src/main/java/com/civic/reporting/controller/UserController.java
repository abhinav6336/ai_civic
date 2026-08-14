package com.civic.reporting.controller;

import com.civic.reporting.dto.request.UserCreateRequest;
import com.civic.reporting.dto.response.ApiResponse;
import com.civic.reporting.dto.response.UserResponse;
import com.civic.reporting.enums.UserRole;
import com.civic.reporting.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) UserRole role) {
        List<UserResponse> users = (role != null) ? userService.getUsersByRole(role) : userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.createUser(request);
        return new ResponseEntity<>(ApiResponse.ok("User created successfully", created), HttpStatus.CREATED);
    }

    @GetMapping("/officers/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getOfficersByDepartment(@PathVariable Long departmentId) {
        List<UserResponse> officers = userService.getOfficersByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.ok(officers));
    }
}
