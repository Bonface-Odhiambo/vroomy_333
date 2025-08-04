package com.insuranceplatform.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "certificate_stock")
public class CertificateStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "superagent_id", nullable = false)
    private Superagent superagent;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private InsuranceCompany insuranceCompany;

    // e.g., "Motor Private", "Travel Gold"
    @Column(nullable = false)
    private String productClass;

    @Column(nullable = false)
    private int quantity;
}