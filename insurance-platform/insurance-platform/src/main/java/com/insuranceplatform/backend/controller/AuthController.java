package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.AuthRequest;
import com.insuranceplatform.backend.dto.AuthResponse;
import com.insuranceplatform.backend.dto.RegisterRequest;
import com.insuranceplatform.backend.service.AuthService;
import com.insuranceplatform.backend.validation.ValidationGroups;
import jakarta.validation.ConstraintViolation; // Import this
import jakarta.validation.Validator; // Import the correct Validator
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // --- THIS IS THE CORRECTED PART ---
    // Inject the Jakarta Validator, not the Spring one
    private final Validator validator;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Validated RegisterRequest request // Basic validation still runs first
    ) {
        // --- THIS IS THE CORRECTED DYNAMIC VALIDATION LOGIC ---
        Class<?> validationGroup = switch (request.getRole()) {
            case ADMIN -> ValidationGroups.Admin.class;
            case SUPERAGENT -> ValidationGroups.Superagent.class;
            case AGENT -> ValidationGroups.Agent.class;
            default -> throw new IllegalStateException("Unexpected value: " + request.getRole());
        };

        // The Jakarta Validator returns a Set of violations
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request, validationGroup);

        if (!violations.isEmpty()) {
            // We can build a better error message if we want, but for now this is fine
            throw new jakarta.validation.ConstraintViolationException(violations);
        }
        // --- END OF CORRECTION ---

        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(
            @RequestBody AuthRequest request
    ) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}