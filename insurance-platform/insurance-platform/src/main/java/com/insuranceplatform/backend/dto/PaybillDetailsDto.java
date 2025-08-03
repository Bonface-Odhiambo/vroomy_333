package com.insuranceplatform.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PaybillDetailsDto {
    private String paybillNumber;
    private String accountNumber;
    private BigDecimal amount;
    private String instructions;
}