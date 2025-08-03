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
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "superagent_id", nullable = false)
    private Superagent superagent;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private InsuranceCompany insuranceCompany;

    @Column(nullable = false)
    private String name; // e.g., "Motor Private", "Travel Standard"

    @Column(nullable = false, precision = 10, scale = 2) // For financial values
    private BigDecimal rate;
}