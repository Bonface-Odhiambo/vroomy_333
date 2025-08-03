package com.insuranceplatform.backend.entity;

import com.insuranceplatform.backend.enums.TransactionStatus;
import com.insuranceplatform.backend.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // --- UPDATED FIELDS ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;
    // --- END OF UPDATED FIELDS ---

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}