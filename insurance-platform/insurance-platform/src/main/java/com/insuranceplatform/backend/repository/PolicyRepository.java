package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Agent;
import com.insuranceplatform.backend.entity.Policy;
import com.insuranceplatform.backend.entity.Superagent;
import com.insuranceplatform.backend.enums.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    // --- Standard Finders ---
    List<Policy> findByAgent(Agent agent);
    List<Policy> findByAgent_SuperagentOrderByCreatedAtDesc(Superagent superagent);
    List<Policy> findByAgentAndExpiryDateBefore(Agent agent, LocalDateTime expiryDate);
    List<Policy> findByAgent_SuperagentAndExpiryDateBefore(Superagent superagent, LocalDateTime expiryDate);

    // --- Counting Methods ---
    long countByAgent_Superagent(Superagent superagent);

    // --- Summation Methods (for Dashboards) ---

    /**
     * Sums the premium amount for all policies with a specific status.
     * Used by the Admin dashboard.
     */
    @Query("SELECT COALESCE(SUM(p.premiumAmount), 0) FROM Policy p WHERE p.status = :status")
    BigDecimal sumPremiumByStatus(@Param("status") PolicyStatus status);

    /**
     * CORRECTED: Sums the premium amount for policies sold by agents of a specific superagent.
     * Used by the Superagent dashboard.
     */
    @Query("SELECT COALESCE(SUM(p.premiumAmount), 0) FROM Policy p WHERE p.agent.superagent = :superagent")
    BigDecimal sumPremiumByAgent_Superagent(@Param("superagent") Superagent superagent);
}