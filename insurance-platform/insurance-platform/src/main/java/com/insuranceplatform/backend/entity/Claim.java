package com.insuranceplatform.backend.entity;

import com.insuranceplatform.backend.enums.ClaimStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(length = 2048) // A field for the agent to describe the incident
    private String description;

    // --- File URLs ---
    @Column(nullable = false)
    private String policeAbstractUrl;

    @Column(nullable = false)
    private String drivingLicenseUrl;

    @Column(nullable = false)
    private String logbookUrl;

    private String photoUrl; // Optional
    private String videoUrl; // Optional

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}