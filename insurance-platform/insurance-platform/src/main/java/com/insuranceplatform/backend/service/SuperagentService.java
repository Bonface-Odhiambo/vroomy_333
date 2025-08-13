package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.*;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.enums.*;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.exception.UserAlreadyExistsException;
import com.insuranceplatform.backend.repository.*;
import com.insuranceplatform.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SuperagentService {

    // --- Dependencies (Unchanged) ---
    private final ProductRepository productRepository;
    private final SuperagentRepository superagentRepository;
    private final InsuranceCompanyRepository companyRepository;
    private final ClaimRepository claimRepository;
    private final NotificationService notificationService;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final TransactionRepository transactionRepository;
    private final CertificateStockRepository certificateStockRepository;
    private final LeadRepository leadRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final MpesaService mpesaService;

    // --- Profile Management, Dashboard, Product Management, Agent Management, Lead Management, Claim Management (All Unchanged) ---
    // (Your existing code for these sections is perfect and has been omitted for brevity)
    @Transactional(readOnly = true)
    public Superagent getCurrentSuperagentProfile() {
        User currentUser = authService.getCurrentUser();
        return getSuperagentProfile(currentUser);
    }

    @Transactional
    public Superagent updateCurrentSuperagentProfile(UpdateProfileRequest request) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        currentUser.setFullName(request.fullName());
        currentUser.setEmail(request.email());
        userRepository.save(currentUser);
        return superagentRepository.save(superagent);
    }
    
    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics() {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        long totalAgents = agentRepository.countBySuperagent(superagent);
        long totalPoliciesSold = policyRepository.countByAgent_Superagent(superagent);
        BigDecimal totalPremium = policyRepository.sumPremiumByAgent_Superagent(superagent);
        long pendingClaims = claimRepository.countClaimsBySuperagentAndStatus(superagent, ClaimStatus.RAISED);
        return DashboardMetricsDto.builder().totalAgents(totalAgents).totalPoliciesSold(totalPoliciesSold).totalPremiumCollected(totalPremium != null ? totalPremium : BigDecimal.ZERO).pendingClaims(pendingClaims).build();
    }
    
    @Transactional(readOnly = true)
    public List<Policy> viewAgentTransactions() {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        return policyRepository.findByAgent_SuperagentOrderByCreatedAtDesc(superagent);
    }
    
    @Transactional(readOnly = true)
    public List<Policy> viewAgentRenewals(int daysUntilExpiry) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        LocalDateTime expiryCutoffDate = LocalDateTime.now().plusDays(daysUntilExpiry);
        return policyRepository.findByAgent_SuperagentAndExpiryDateBefore(superagent, expiryCutoffDate);
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        InsuranceCompany company = companyRepository.findById(request.getCompanyId()).orElseThrow(() -> new ResourceNotFoundException("InsuranceCompany not found with ID: " + request.getCompanyId()));
        Product product = Product.builder().superagent(superagent).insuranceCompany(company).name(request.getName()).rate(request.getRate()).calculationType(request.getCalculationType()).build();
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> viewMyProducts() {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        return productRepository.findBySuperagent(superagent);
    }

    @Transactional
    public Product updateProduct(Long productId, ProductRequest request) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        if (!product.getSuperagent().getId().equals(superagent.getId())) { throw new SecurityException("You are not authorized to update this product."); }
        InsuranceCompany company = companyRepository.findById(request.getCompanyId()).orElseThrow(() -> new ResourceNotFoundException("InsuranceCompany not found with ID: " + request.getCompanyId()));
        product.setName(request.getName());
        product.setRate(request.getRate());
        product.setInsuranceCompany(company);
        product.setCalculationType(request.getCalculationType());
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        if (!product.getSuperagent().getId().equals(superagent.getId())) { throw new SecurityException("You are not authorized to delete this product."); }
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<CertificateStock> viewCertificateStock() {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        return certificateStockRepository.findBySuperagent(superagent);
    }

    @Transactional
    public Agent createAgent(CreateAgentRequest request) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        if (userRepository.existsByEmail(request.email())) { throw new UserAlreadyExistsException("An account with this email already exists: " + request.email()); }
        if (userRepository.existsByPhone(request.phone())) { throw new UserAlreadyExistsException("An account with this phone number already exists: " + request.phone()); }
        User newAgentUser = new User();
        newAgentUser.setFullName(request.fullName());
        newAgentUser.setEmail(request.email());
        newAgentUser.setPhone(request.phone());
        newAgentUser.setRole(UserRole.AGENT);
        newAgentUser.setStatus(UserStatus.ACTIVE);
        newAgentUser.setPassword(passwordEncoder.encode("Password123"));
        User savedUser = userRepository.save(newAgentUser);
        Agent newAgent = new Agent();
        newAgent.setUser(savedUser);
        newAgent.setSuperagent(superagent);
        return agentRepository.save(newAgent);
    }

    @Transactional(readOnly = true)
    public List<Agent> viewMyAgents() {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        return agentRepository.findBySuperagent(superagent);
    }

    @Transactional
    public User updateAgentStatus(Long agentUserId, UpdateAgentStatusRequest request) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        User agentUser = userRepository.findById(agentUserId).orElseThrow(() -> new ResourceNotFoundException("Agent user not found with ID: " + agentUserId));
        Agent agentProfile = agentRepository.findByUser(agentUser).orElseThrow(() -> new ResourceNotFoundException("Agent profile not found for user ID: " + agentUserId));
        if (!agentProfile.getSuperagent().getId().equals(superagent.getId())) { throw new SecurityException("You are not authorized to manage this agent."); }
        agentUser.setStatus(request.getStatus());
        User updatedUser = userRepository.save(agentUser);
        String message = String.format("Your account status has been updated to %s by your Superagent.", request.getStatus());
        notificationService.createNotification(currentUser, agentUser, message);
        return updatedUser;
    }

    @Transactional
    public Lead createLead(LeadRequest request) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        Lead lead = Lead.builder().superagent(superagent).customerName(request.getCustomerName()).customerPhone(request.getCustomerPhone()).customerEmail(request.getCustomerEmail()).notes(request.getNotes()).status(request.getStatus()).build();
        return leadRepository.save(lead);
    }

    @Transactional(readOnly = true)
    public List<Lead> viewLeads() {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        return leadRepository.findBySuperagentOrderByCreatedAtDesc(superagent);
    }

    @Transactional
    public Lead updateLead(Long leadId, LeadRequest request) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        Lead lead = leadRepository.findById(leadId).orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + leadId));
        if (!lead.getSuperagent().getId().equals(superagent.getId())) { throw new SecurityException("You are not authorized to update this lead."); }
        lead.setCustomerName(request.getCustomerName());
        lead.setCustomerPhone(request.getCustomerPhone());
        lead.setCustomerEmail(request.getCustomerEmail());
        lead.setNotes(request.getNotes());
        lead.setStatus(request.getStatus());
        lead.setUpdatedAt(LocalDateTime.now());
        return leadRepository.save(lead);
    }

    public void deleteLead(Long leadId) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        Lead lead = leadRepository.findById(leadId).orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + leadId));
        if (!lead.getSuperagent().getId().equals(superagent.getId())) { throw new SecurityException("You are not authorized to delete this lead."); }
        leadRepository.delete(lead);
    }

    @Transactional(readOnly = true)
    public List<Claim> viewClaims() {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        return claimRepository.findClaimsBySuperagent(superagent);
    }
    
    @Transactional
    public Claim updateClaimStatus(Long claimId, UpdateClaimStatusRequest request) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new ResourceNotFoundException("Claim not found with ID: " + claimId));
        if (!claim.getPolicy().getAgent().getSuperagent().getId().equals(superagent.getId())) { throw new SecurityException("You are not authorized to manage this claim."); }
        claim.setStatus(request.getStatus());
        claim.setUpdatedAt(LocalDateTime.now());
        Claim updatedClaim = claimRepository.save(claim);
        User agentUser = claim.getPolicy().getAgent().getUser();
        String message = String.format("Your claim (ID: %d) has been updated to status: %s.", updatedClaim.getId(), updatedClaim.getStatus());
        notificationService.createNotification(currentUser, agentUser, message);
        return updatedClaim;
    }

    // --- Payout Management (UPDATED FOR M-PESA B2C) ---

    @Transactional(readOnly = true)
    public List<Transaction> viewPendingWithdrawals() {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        return transactionRepository.findPendingWithdrawalsForSuperagent(superagent);
    }

    @Transactional
    public Transaction approveWithdrawal(ApproveWithdrawalDto request) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        Transaction withdrawalTx = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal transaction not found with ID: " + request.getTransactionId()));

        if (withdrawalTx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("This transaction is not in a pending state.");
        }

        Agent agent = withdrawalTx.getWallet().getUser().getAgentProfile();
        if (agent == null || !agent.getSuperagent().getId().equals(superagent.getId())) {
            throw new SecurityException("You are not authorized to approve this withdrawal.");
        }

        BigDecimal amountToPay = withdrawalTx.getAmount();
        String agentPhoneNumber = agent.getUser().getPhone();
        String remarks = "Commission Payout for Transaction ID: " + withdrawalTx.getId();
        Long transactionId = withdrawalTx.getId(); // Get the transaction ID

        // CORRECTED: Pass the transactionId as the third argument
        mpesaService.initiateB2CPayment(amountToPay, agentPhoneNumber, remarks, transactionId);
        
        withdrawalTx.setTransactionType(TransactionType.WITHDRAWAL_PROCESSING);
        
        String message = String.format("Your withdrawal request for KES %.2f has been approved and is being processed via M-Pesa.", amountToPay);
        notificationService.createNotification(currentUser, agent.getUser(), message);
        
        return transactionRepository.save(withdrawalTx);
    }

    // --- Document Management (Unchanged) ---

    @Transactional(readOnly = true)
    public List<DocumentDto> viewPolicyDocuments(Long policyId) {
        User currentUser = authService.getCurrentUser();
        Superagent superagent = getSuperagentProfile(currentUser);
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + policyId));
        if (!policy.getAgent().getSuperagent().getId().equals(superagent.getId())) { throw new SecurityException("You are not authorized to view documents for this policy."); }
        List<DocumentDto> documents = new ArrayList<>();
        if (policy.getClient().getIdFileUrl() != null) documents.add(new DocumentDto("Client ID/KRA", policy.getClient().getIdFileUrl(), "Client identification document."));
        if (policy.getLogbookFileUrl() != null) documents.add(new DocumentDto("Motor Logbook", policy.getLogbookFileUrl(), "Vehicle registration logbook."));
        if (policy.getCertificateUrl() != null) documents.add(new DocumentDto("Policy Certificate", policy.getCertificateUrl(), "Official insurance policy certificate."));
        Optional<Claim> claimOpt = claimRepository.findByPolicy(policy);
        if (claimOpt.isPresent()) {
            Claim claim = claimOpt.get();
            if (claim.getPoliceAbstractUrl() != null) documents.add(new DocumentDto("Police Abstract", claim.getPoliceAbstractUrl(), "Claim document: Police Abstract"));
            if (claim.getDrivingLicenseUrl() != null) documents.add(new DocumentDto("Driving License", claim.getDrivingLicenseUrl(), "Claim document: Driver's License"));
            if (claim.getPhotoUrl() != null) documents.add(new DocumentDto("Accident Photo", claim.getPhotoUrl(), "Claim document: Photo of incident"));
            if (claim.getVideoUrl() != null) documents.add(new DocumentDto("Accident Video", claim.getVideoUrl(), "Claim document: Video of incident"));
        }
        return documents;
    }

    // --- Private Helper Method (Unchanged) ---
    private Superagent getSuperagentProfile(User currentUser) {
        return superagentRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Superagent profile not found for the current user."));
    }
}