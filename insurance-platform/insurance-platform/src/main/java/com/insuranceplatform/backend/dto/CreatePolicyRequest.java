package com.insuranceplatform.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePolicyRequest {
    private String clientFullName;
    private String clientIdentifier; // e.g., KRA PIN or ID number
    private Long productId;
    private BigDecimal insuredValue; // e.g., The value of the car for motor insurance
}