package com.insuranceplatform.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MpesaCallbackRequest {
    // This is a simplified version of the real Safaricom callback
    private int resultCode; // 0 for success
    private String resultDesc;
    private Long policyId; // We add this for our simulation
    private BigDecimal amount;
    private String mpesaReceiptNumber;
}