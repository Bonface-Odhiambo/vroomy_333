package com.insuranceplatform.backend.dto;

import com.insuranceplatform.backend.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAgentStatusRequest {
    @NotNull(message = "Status cannot be null")
    private UserStatus status;
}