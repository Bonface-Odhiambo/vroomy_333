package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Claim;
import com.insuranceplatform.backend.entity.Policy;
import com.insuranceplatform.backend.entity.Superagent;
import com.insuranceplatform.backend.entity.Agent;
import com.insuranceplatform.backend.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List; // Added missing import
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /**
     * Finds a claim associated with a specific policy.
     *
     * @param policy The policy to find the claim for.
     * @return An Optional containing the Claim if found.
     */
    Optional<Claim> findByPolicy(Policy policy);

    /**
     * A custom query to find all claims belonging to agents managed by a specific superagent,
     * ordered by the most recently created.
     *
     * @param superagent The superagent whose agents' claims are to be retrieved.
     * @return A list of claims.
     */
    @Query("SELECT c FROM Claim c WHERE c.policy.agent.superagent = :superagent ORDER BY c.createdAt DESC")
    List<Claim> findClaimsBySuperagent(@Param("superagent") Superagent superagent);

    /**
     * Counts the total number of claims that have a status within the provided list.
     * Used by the Admin dashboard to count pending claims.
     *
     * @param statuses A list of ClaimStatus enums.
     * @return The total count of claims matching the statuses.
     */
    long countByStatusIn(List<ClaimStatus> statuses);

    /**
     * A custom query to count claims for a specific superagent that match a given status.
     * Used by the Superagent dashboard.
     *
     * @param superagent The superagent to count claims for.
     * @param status     The status of the claims to count.
     * @return The total count of matching claims.
     */
    @Query("SELECT count(c) FROM Claim c WHERE c.policy.agent.superagent = :superagent AND c.status = :status")
    long countClaimsBySuperagentAndStatus(@Param("superagent") Superagent superagent, @Param("status") ClaimStatus status);
    List<Claim> findByPolicy_Agent(Agent agent);
}