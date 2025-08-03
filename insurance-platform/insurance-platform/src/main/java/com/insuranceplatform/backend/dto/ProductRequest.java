package com.insuranceplatform.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

import com.insuranceplatform.backend.enums.CalculationType;

@Data
public class ProductRequest {
    private String name;
    private BigDecimal rate;
    private Long companyId;
    private CalculationType calculationType;
}