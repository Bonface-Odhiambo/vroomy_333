package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.CertificateStock;
import com.insuranceplatform.backend.entity.InsuranceCompany;
import com.insuranceplatform.backend.entity.Superagent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateStockRepository extends JpaRepository<CertificateStock, Long> {
    // Find stock for a specific superagent
    List<CertificateStock> findBySuperagent(Superagent superagent);

    // Find a specific stock item to deduct from
    Optional<CertificateStock> findBySuperagentAndInsuranceCompanyAndProductClass(
            Superagent superagent, InsuranceCompany insuranceCompany, String productClass);
}