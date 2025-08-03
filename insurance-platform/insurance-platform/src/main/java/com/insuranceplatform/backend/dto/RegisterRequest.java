package com.insuranceplatform.backend.dto;

import com.insuranceplatform.backend.enums.UserRole;
import com.insuranceplatform.backend.validation.ValidationGroups;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor // <-- We are adding this back
public class RegisterRequest {

    // --- Fields for ALL roles ---
    @NotBlank(message = "Full name cannot be blank")
    private String fullName;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Phone number cannot be blank")
    private String phone;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotNull(message = "User role must be provided")
    private UserRole role;

    // --- Superagent-specific fields ---
    @NotBlank(groups = ValidationGroups.Superagent.class, message = "IRA number is required for Superagents")
    @Null(groups = {ValidationGroups.Admin.class, ValidationGroups.Agent.class}, message = "IRA number should not be provided for this role")
    private String iraNumber;

    @NotBlank(groups = ValidationGroups.Superagent.class, message = "KRA PIN is required for Superagents")
    @Null(groups = {ValidationGroups.Admin.class, ValidationGroups.Agent.class}, message = "KRA PIN should not be provided for this role")
    private String kraPin;

    @NotBlank(groups = ValidationGroups.Superagent.class, message = "Paybill number is required for Superagents")
    @Null(groups = {ValidationGroups.Admin.class, ValidationGroups.Agent.class}, message = "Paybill number should not be provided for this role")
    private String paybillNumber;

    // --- Agent-specific fields ---
    @NotNull(groups = ValidationGroups.Agent.class, message = "Superagent ID is required for Agents")
    @Null(groups = {ValidationGroups.Admin.class, ValidationGroups.Superagent.class}, message = "Superagent ID should not be provided for this role")
    private Long superagentId;

    // We have removed the manual constructor. Lombok will now generate the correct one.
}