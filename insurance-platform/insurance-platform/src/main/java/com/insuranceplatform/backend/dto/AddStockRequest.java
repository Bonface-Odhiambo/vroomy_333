package com.insuranceplatform.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddStockRequest {
    @NotNull
    private Long superagentId;
    @NotNull
    private Long companyId;
    @NotBlank
    private String productClass;
    @Min(1)
    private int quantity;
}