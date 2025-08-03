package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Agent;
import com.insuranceplatform.backend.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.insuranceplatform.backend.entity.Superagent;

import java.time.LocalDateTime; 
import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    // A finder to get all policies for a specific agent
    List<Policy> findByAgent(Agent agent);
    List<Policy> findByAgent_SuperagentOrderByCreatedAtDesc(Superagent superagent);
    List<Policy> findByAgentAndExpiryDateBefore(Agent agent, LocalDateTime expiryDate);
    List<Policy> findByAgent_SuperagentAndExpiryDateBefore(Superagent superagent, LocalDateTime expiryDate);
}