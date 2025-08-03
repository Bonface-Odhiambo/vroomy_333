package com.insuranceplatform.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TaxRateRequest {
    private BigDecimal taxRate;
}