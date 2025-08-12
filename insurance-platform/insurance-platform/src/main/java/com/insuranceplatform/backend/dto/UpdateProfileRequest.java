package com.insuranceplatform.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank(message = "Full name cannot be blank")
    String fullName,

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    String email
) {}