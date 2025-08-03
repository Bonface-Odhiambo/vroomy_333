package com.insuranceplatform.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WithdrawalRequestDto {
    @DecimalMin(value = "1.00", message = "Withdrawal amount must be greater than 0")
    private BigDecimal amount;
}