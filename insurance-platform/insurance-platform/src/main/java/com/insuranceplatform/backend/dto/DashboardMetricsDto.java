package com.insuranceplatform.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardMetricsDto {
    private long totalUsers;
    private long totalSuperagents;
    private long totalAgents;
    private long totalPoliciesSold;
    private long pendingClaims;
    private BigDecimal totalPremiumCollected;
    private BigDecimal totalCommissionsPaidOut;
}