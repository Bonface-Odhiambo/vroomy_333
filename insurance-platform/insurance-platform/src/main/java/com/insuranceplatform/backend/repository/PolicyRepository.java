package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Agent;
import com.insuranceplatform.backend.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    // A finder to get all policies for a specific agent
    List<Policy> findByAgent(Agent agent);
}