package com.insuranceplatform.backend.dto;

import com.insuranceplatform.backend.enums.ClaimStatus;
import lombok.Data;

@Data
public class UpdateClaimStatusRequest {
    private ClaimStatus status;
}