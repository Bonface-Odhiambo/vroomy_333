package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.*;
import com.insuranceplatform.backend.service.AuthService;
import com.insuranceplatform.backend.validation.ValidationGroups;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
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
    private final Validator validator;

    // --- Core Authentication ---

    /**
     * Registers a new user with dynamic validation based on their role.
     * Preserves your custom validation group logic.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Validated RegisterRequest request) {
        Class<?> validationGroup = switch (request.getRole()) {
            case ADMIN -> ValidationGroups.Admin.class;
            case SUPERAGENT -> ValidationGroups.Superagent.class;
            case AGENT -> ValidationGroups.Agent.class;
            default -> throw new IllegalStateException("Unexpected role value: " + request.getRole());
        };

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request, validationGroup);

        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }

        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Authenticates a user and returns a JWT access and refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        // CORRECTED: Calling the 'login' method in the service, not 'authenticate'.
        return ResponseEntity.ok(authService.login(request));
    }
    
    /**
     * Handles user logout. Primarily a signal for the client to clear tokens.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.ok().build();
    }

    // --- Password Management ---

    /**
     * Initiates the password reset process for a user.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Completes the password reset process using a token and a new password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    // --- Token Management ---

    /**
     * Provides a new access token using a valid refresh token.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}