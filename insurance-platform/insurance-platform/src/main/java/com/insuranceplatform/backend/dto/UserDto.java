package com.insuranceplatform.backend.dto;
public record UserDto(
    Long id,
    String fullName,
    String email,
    String role, // e.g., "AGENT", "SUPERAGENT"
    String status // e.g., "ACTIVE", "PENDING_APPROVAL", "DEACTIVATED"
) {}