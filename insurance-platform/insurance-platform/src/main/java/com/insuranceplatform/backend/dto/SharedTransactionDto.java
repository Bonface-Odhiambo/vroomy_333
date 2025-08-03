package com.insuranceplatform.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SharedTransactionDto {
    private Long policyId;
    private String productName;
    private BigDecimal totalAmount;
    private LocalDateTime paidAt;
    private String agentName;
    private String superagentName;
}