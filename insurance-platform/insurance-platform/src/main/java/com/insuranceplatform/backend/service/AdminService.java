package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.CompanyRequest;
import com.insuranceplatform.backend.dto.DashboardMetricsDto;
import com.insuranceplatform.backend.dto.TaxRateRequest;
import com.insuranceplatform.backend.dto.UserStatusRequest;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.enums.ClaimStatus;
import com.insuranceplatform.backend.enums.PolicyStatus;
import com.insuranceplatform.backend.enums.TransactionType;
import com.insuranceplatform.backend.enums.UserRole;
import com.insuranceplatform.backend.enums.UserStatus;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.*;
import com.insuranceplatform.backend.dto.AddStockRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final InsuranceCompanyRepository companyRepository;
    private final GlobalConfigRepository configRepository;
    private final UserRepository userRepository;
    private final SuperagentRepository superagentRepository;
    private final AgentRepository agentRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final TransactionRepository transactionRepository;
    private final CertificateStockRepository certificateStockRepository;

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
                .orElse(new GlobalConfig());

        config.setId(GLOBAL_CONFIG_ID);
        config.setTaxRate(request.getTaxRate());
        return configRepository.save(config);
    }

    public GlobalConfig getGlobalConfig() {
        return configRepository.findById(GLOBAL_CONFIG_ID)
                .orElseThrow(() -> new ResourceNotFoundException("GlobalConfig not found. Please set it first."));
    }

    // --- User Management ---
    @Transactional
    public User updateUserStatus(Long userId, UserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setStatus(request.getStatus());
        User updatedUser = userRepository.save(user);

        if (user.getRole() == UserRole.SUPERAGENT && request.getStatus() == UserStatus.INACTIVE) {
            Superagent superagent = superagentRepository.findByUser(user)
                    .orElseThrow(() -> new IllegalStateException("Superagent profile not found for user: " + user.getFullName()));
            
            List<Agent> agentsToDeactivate = agentRepository.findBySuperagent(superagent);

            for (Agent agent : agentsToDeactivate) {
                User agentUser = agent.getUser();
                agentUser.setStatus(UserStatus.INACTIVE);
                userRepository.save(agentUser);
            }
        }
        return updatedUser;
    }

    @Transactional
    public CertificateStock addCertificateStock(AddStockRequest request) {
        Superagent superagent = superagentRepository.findById(request.getSuperagentId())
                .orElseThrow(() -> new ResourceNotFoundException("Superagent not found with ID: " + request.getSuperagentId()));
        
        InsuranceCompany company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Insurance Company not found with ID: " + request.getCompanyId()));

        // Check if stock for this combination already exists. If so, update it.
        CertificateStock stock = certificateStockRepository
                .findBySuperagentAndInsuranceCompanyAndProductClass(superagent, company, request.getProductClass())
                .orElse(new CertificateStock()); // Or create a new one

        stock.setSuperagent(superagent);
        stock.setInsuranceCompany(company);
        stock.setProductClass(request.getProductClass());
        stock.setQuantity(stock.getQuantity() + request.getQuantity()); // Add to existing quantity

        return certificateStockRepository.save(stock);
    }

    // --- Dashboard Metrics ---
    public DashboardMetricsDto getDashboardMetrics() {
        long totalUsers = userRepository.count();
        long totalSuperagents = superagentRepository.count();
        long totalAgents = agentRepository.count();
        long totalPoliciesSold = policyRepository.count();
        
        BigDecimal totalPremium = policyRepository.sumPremiumByStatus(PolicyStatus.PAID);
        BigDecimal commissionsPaid = transactionRepository.sumAmountByTransactionType(TransactionType.COMMISSION_EARNED);
        
        List<ClaimStatus> pendingStatuses = List.of(ClaimStatus.RAISED, ClaimStatus.IN_REVIEW);
        long pendingClaims = claimRepository.countByStatusIn(pendingStatuses);

        return DashboardMetricsDto.builder()
                .totalUsers(totalUsers)
                .totalSuperagents(totalSuperagents)
                .totalAgents(totalAgents)
                .totalPoliciesSold(totalPoliciesSold)
                .totalPremiumCollected(totalPremium)
                .totalCommissionsPaidOut(commissionsPaid)
                .pendingClaims(pendingClaims)
                .build();
    }

    // --- API Key Management ---
    public ApiKey generateApiKey(Long companyId) {
        InsuranceCompany company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceCompany not found with id: " + companyId));
        
        String key = UUID.randomUUID().toString();
        
        ApiKey apiKey = ApiKey.builder()
                .keyValue(key)
                .insuranceCompany(company)
                .build();
        
        return apiKeyRepository.save(apiKey);
    }
}