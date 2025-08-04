package com.insuranceplatform.backend.dto;

import com.insuranceplatform.backend.enums.LeadStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeadRequest {
    @NotBlank(message = "Customer name is required")
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String notes;
    @NotNull(message = "Lead status is required")
    private LeadStatus status;
}