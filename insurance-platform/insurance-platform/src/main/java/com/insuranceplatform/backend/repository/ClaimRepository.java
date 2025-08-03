package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Claim;
import com.insuranceplatform.backend.entity.Superagent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.insuranceplatform.backend.enums.ClaimStatus; 
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    // A custom query to find all claims belonging to agents managed by a specific superagent
    @Query("SELECT c FROM Claim c WHERE c.policy.agent.superagent = :superagent ORDER BY c.createdAt DESC")
    List<Claim> findClaimsBySuperagent(Superagent superagent);
    long countByStatusIn(List<ClaimStatus> statuses);
}