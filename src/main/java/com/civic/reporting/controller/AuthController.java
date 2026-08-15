package com.civic.reporting.controller;

import com.civic.reporting.dto.request.LoginRequest;
import com.civic.reporting.dto.response.ApiResponse;
import com.civic.reporting.dto.response.AuthResponse;
import com.civic.reporting.entity.User;
import com.civic.reporting.exception.BadRequestException;
import com.civic.reporting.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
@Transactional(readOnly = true)
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        String identifier = request.getEmail().trim().toLowerCase();
        
        // Support shorthand usernames like 'admin'
        if ("admin".equals(identifier)) {
            identifier = "admin@civic.gov";
        }
        
        final String searchEmail = identifier;
        User user = userRepository.findByEmail(searchEmail)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> u.getEmail().equalsIgnoreCase(searchEmail) || u.getName().equalsIgnoreCase(searchEmail))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Invalid email/username or password.")));

        // Verify password
        if (user.getPassword() != null && !user.getPassword().equals(request.getPassword().trim())) {
            throw new BadRequestException("Invalid email/username or password.");
        }

        // Generate session token
        String token = "CIVIC-AUTH-" + Base64.getEncoder().encodeToString((user.getId() + ":" + user.getEmail() + ":" + UUID.randomUUID()).getBytes());

        AuthResponse authResponse = AuthResponse.fromUser(user, token);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) Long userId) {

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BadRequestException("User session not found"));
            return ResponseEntity.ok(ApiResponse.ok(AuthResponse.fromUser(user, "CIVIC-AUTH-ACTIVE")));
        }

        if (authHeader != null && authHeader.startsWith("Bearer CIVIC-AUTH-")) {
            try {
                String raw = authHeader.replace("Bearer CIVIC-AUTH-", "");
                String decoded = new String(Base64.getDecoder().decode(raw));
                String[] parts = decoded.split(":");
                Long id = Long.parseLong(parts[0]);
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new BadRequestException("User not found"));
                return ResponseEntity.ok(ApiResponse.ok(AuthResponse.fromUser(user, authHeader)));
            } catch (Exception ignored) {
            }
        }

        throw new BadRequestException("Not authenticated");
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", "SESSION_TERMINATED"));
    }
}
