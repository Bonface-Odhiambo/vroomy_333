package com.insuranceplatform.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    private String name;
    private BigDecimal rate;
    private Long companyId;
}