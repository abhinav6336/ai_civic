package com.civic.reporting.service.impl;

import com.civic.reporting.dto.request.UserCreateRequest;
import com.civic.reporting.dto.response.UserResponse;
import com.civic.reporting.entity.Department;
import com.civic.reporting.entity.User;
import com.civic.reporting.enums.UserRole;
import com.civic.reporting.exception.BadRequestException;
import com.civic.reporting.exception.ResourceNotFoundException;
import com.civic.reporting.repository.DepartmentRepository;
import com.civic.reporting.repository.UserRepository;
import com.civic.reporting.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public UserServiceImpl(UserRepository userRepository, DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(getUserEntity(id));
    }

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("A user with this email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.CITIZEN);

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));
            user.setDepartment(dept);
        }

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Override
    public User getOrCreateCitizen(Long citizenId, String name, String email, String phone) {
        if (citizenId != null) {
            return getUserEntity(citizenId);
        }

        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setName(name != null && !name.isBlank() ? name : "Citizen User");
                newUser.setEmail(email);
                newUser.setPhone(phone);
                newUser.setRole(UserRole.CITIZEN);
                return userRepository.save(newUser);
            });
        }

        // Default citizen fallback
        return userRepository.findByEmail("citizen.default@civic-portal.org").orElseGet(() -> {
            User defaultCitizen = new User();
            defaultCitizen.setName("Default Citizen");
            defaultCitizen.setEmail("citizen.default@civic-portal.org");
            defaultCitizen.setPhone("+1-555-0100");
            defaultCitizen.setRole(UserRole.CITIZEN);
            return userRepository.save(defaultCitizen);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getOfficersByDepartment(Long departmentId) {
        return userRepository.findByDepartmentId(departmentId).stream()
                .filter(u -> u.getRole() == UserRole.OFFICER)
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
