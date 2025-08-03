package com.insuranceplatform.backend.dto;

import lombok.Data;

@Data
public class RaiseClaimRequest {
    private Long policyId;
    private String description;
}