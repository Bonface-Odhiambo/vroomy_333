package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.InsuranceCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceCompanyRepository extends JpaRepository<InsuranceCompany, Long> {
    // We can add custom finders here if needed, e.g., findByName
}