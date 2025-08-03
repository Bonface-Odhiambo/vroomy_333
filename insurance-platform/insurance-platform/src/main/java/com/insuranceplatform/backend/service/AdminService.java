package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.CompanyRequest;
import com.insuranceplatform.backend.dto.TaxRateRequest;
import com.insuranceplatform.backend.dto.UserStatusRequest;
import com.insuranceplatform.backend.entity.*; // Import all entities
import com.insuranceplatform.backend.enums.UserRole; // Import UserRole
import com.insuranceplatform.backend.enums.UserStatus; // Import UserStatus
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.*; // Import all repositories
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final InsuranceCompanyRepository companyRepository;
    private final GlobalConfigRepository configRepository;
    private final UserRepository userRepository;
    private final SuperagentRepository superagentRepository; // Inject SuperagentRepository
    private final AgentRepository agentRepository; // Inject AgentRepository

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
    @Transactional // Add Transactional to ensure all or no updates happen
    public User updateUserStatus(Long userId, UserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Update the target user's status first
        user.setStatus(request.getStatus());
        User updatedUser = userRepository.save(user);

        // --- THIS IS THE NEW CASCADE LOGIC ---
        // If the user is a Superagent AND the new status is DEACTIVATED,
        // then deactivate all of their agents as well.
        if (user.getRole() == UserRole.SUPERAGENT && request.getStatus() == UserStatus.INACTIVE) { // Assuming DEACTIVATED is INACTIVE
            Superagent superagent = superagentRepository.findByUser(user)
                    .orElseThrow(() -> new IllegalStateException("Superagent profile not found for user: " + user.getFullName()));
            
            List<Agent> agentsToDeactivate = agentRepository.findBySuperagent(superagent);

            for (Agent agent : agentsToDeactivate) {
                User agentUser = agent.getUser();
                agentUser.setStatus(UserStatus.INACTIVE);
                userRepository.save(agentUser);
            }
        }
        // --- END OF CASCADE LOGIC ---

        return updatedUser;
    }

    // --- Dashboard/Metrics (Placeholder) ---
    public String getDashboardMetrics() {
        long totalUsers = userRepository.count();
        long totalCompanies = companyRepository.count();
        return String.format("{\"totalUsers\": %d, \"totalCompanies\": %d}", totalUsers, totalCompanies);
    }
}