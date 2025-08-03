package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.CompanyRequest;
import com.insuranceplatform.backend.dto.TaxRateRequest;
import com.insuranceplatform.backend.dto.UserStatusRequest;
import com.insuranceplatform.backend.entity.GlobalConfig;
import com.insuranceplatform.backend.entity.InsuranceCompany;
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.GlobalConfigRepository;
import com.insuranceplatform.backend.repository.InsuranceCompanyRepository;
import com.insuranceplatform.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final InsuranceCompanyRepository companyRepository;
    private final GlobalConfigRepository configRepository;
    private final UserRepository userRepository;

    private static final Long GLOBAL_CONFIG_ID = 1L;

    // --- Insurance Company Management ---

    public InsuranceCompany createCompany(CompanyRequest request) {
        InsuranceCompany company = InsuranceCompany.builder()
                .name(request.getName())
                .iraNumber(request.getIraNumber())
                .build();
        return companyRepository.save(company);
    }

    public List<InsuranceCompany> getAllCompanies() {
        return companyRepository.findAll();
    }

    public InsuranceCompany getCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceCompany not found with id: " + id));
    }

    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new ResourceNotFoundException("InsuranceCompany not found with id: " + id);
        }
        companyRepository.deleteById(id);
    }

    // --- Global Config Management ---

    public GlobalConfig setTaxRate(TaxRateRequest request) {
        GlobalConfig config = configRepository.findById(GLOBAL_CONFIG_ID)
                .orElse(new GlobalConfig()); // Create new if it doesn't exist

        config.setId(GLOBAL_CONFIG_ID);
        config.setTaxRate(request.getTaxRate());
        return configRepository.save(config);
    }

    public GlobalConfig getGlobalConfig() {
        return configRepository.findById(GLOBAL_CONFIG_ID)
                .orElseThrow(() -> new ResourceNotFoundException("GlobalConfig not found. Please set it first."));
    }

    // --- User Management ---

    public User updateUserStatus(Long userId, UserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setStatus(request.getStatus());
        return userRepository.save(user);
    }

    // --- Dashboard/Metrics (Placeholder) ---

    public String getDashboardMetrics() {
        // In a real application, this would query various repositories and aggregate data.
        long totalUsers = userRepository.count();
        long totalCompanies = companyRepository.count();
        return String.format("{\"totalUsers\": %d, \"totalCompanies\": %d}", totalUsers, totalCompanies);
    }
}