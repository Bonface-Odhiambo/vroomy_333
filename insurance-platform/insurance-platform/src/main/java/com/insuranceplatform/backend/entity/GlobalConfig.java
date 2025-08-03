package com.insuranceplatform.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "global_config")
public class GlobalConfig {

    @Id
    private Long id; // We will always use ID=1 for the single config row

    @Column(nullable = false, precision = 5, scale = 2) // e.g., 16.00
    private BigDecimal taxRate;

    // We can add other global settings here later
    // private String someOtherSetting;
}