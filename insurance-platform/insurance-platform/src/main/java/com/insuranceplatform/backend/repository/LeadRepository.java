package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Lead;
import com.insuranceplatform.backend.entity.Superagent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    // Find all leads for a specific superagent
    List<Lead> findBySuperagentOrderByCreatedAtDesc(Superagent superagent);
}