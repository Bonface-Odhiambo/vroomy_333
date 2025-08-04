package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.*; // Import all DTOs
import com.insuranceplatform.backend.entity.*; // Import all Entities
import com.insuranceplatform.backend.enums.TransactionStatus;
import com.insuranceplatform.backend.enums.TransactionType;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.*; // Import all Repositories
import lombok.RequiredArgsConstructor;
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

    // All required repositories injected via the constructor
    private final ProductRepository productRepository;
    private final SuperagentRepository superagentRepository;
    private final InsuranceCompanyRepository companyRepository;
    private final ClaimRepository claimRepository;
    private final NotificationService notificationService;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CertificateStockRepository certificateStockRepository; 
    private final LeadRepository leadRepository;

    // Helper method to get the Superagent profile for the logged-in user
    private Superagent getSuperagentProfile(User currentUser) {
        return superagentRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Superagent profile not found for the current user."));
    }

    // --- Product Management ---
    @Transactional
    public Product createProduct(ProductRequest request, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        InsuranceCompany company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceCompany not found with ID: " + request.getCompanyId()));

        Product product = Product.builder()
                .superagent(superagent)
                .insuranceCompany(company)
                .name(request.getName())
                .rate(request.getRate())
                .calculationType(request.getCalculationType())
                .build();

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<CertificateStock> viewCertificateStock(User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        return certificateStockRepository.findBySuperagent(superagent);
    }
    // --- Agent Management ---
    @Transactional(readOnly = true)
    public List<Agent> viewMyAgents(User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        return agentRepository.findBySuperagent(superagent);
    }

    @Transactional
    public User updateAgentStatus(Long agentUserId, UpdateAgentStatusRequest request, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        User agentUser = userRepository.findById(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent user not found with ID: " + agentUserId));
        Agent agentProfile = agentRepository.findByUser(agentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found for user ID: " + agentUserId));

        if (!agentProfile.getSuperagent().getId().equals(superagent.getId())) {
            throw new SecurityException("You are not authorized to manage this agent.");
        }

        agentUser.setStatus(request.getStatus());
        User updatedUser = userRepository.save(agentUser);
        String message = String.format("Your account status has been updated to %s by your Superagent.", request.getStatus());
        notificationService.createNotification(currentUser, agentUser, message);

        return updatedUser;
    }

    // --- Reporting & Renewals ---
    @Transactional(readOnly = true)
    public List<Policy> viewAgentTransactions(User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        return policyRepository.findByAgent_SuperagentOrderByCreatedAtDesc(superagent);
    }

    @Transactional(readOnly = true)
    public List<Policy> viewAgentRenewals(User currentUser, int daysUntilExpiry) {
        Superagent superagent = getSuperagentProfile(currentUser);
        LocalDateTime expiryCutoffDate = LocalDateTime.now().plusDays(daysUntilExpiry);
        return policyRepository.findByAgent_SuperagentAndExpiryDateBefore(superagent, expiryCutoffDate);
    }

    // --- Document Management ---
    @Transactional(readOnly = true)
    public List<DocumentDto> viewPolicyDocuments(Long policyId, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + policyId));

        if (!policy.getAgent().getSuperagent().getId().equals(superagent.getId())) {
            throw new SecurityException("You are not authorized to view documents for this policy.");
        }

        List<DocumentDto> documents = new ArrayList<>();

        if (policy.getClient().getIdFileUrl() != null) {
            documents.add(new DocumentDto("Client ID/KRA", policy.getClient().getIdFileUrl(), "Client identification document."));
        }
        if (policy.getLogbookFileUrl() != null) {
            documents.add(new DocumentDto("Motor Logbook", policy.getLogbookFileUrl(), "Vehicle registration logbook."));
        }
        if (policy.getCertificateUrl() != null) {
            documents.add(new DocumentDto("Policy Certificate", policy.getCertificateUrl(), "Official insurance policy certificate."));
        }

        Optional<Claim> claimOpt = claimRepository.findAll().stream().filter(c -> c.getPolicy().getId().equals(policyId)).findFirst();
        if (claimOpt.isPresent()) {
            Claim claim = claimOpt.get();
            documents.add(new DocumentDto("Police Abstract", claim.getPoliceAbstractUrl(), "Claim document: Police Abstract"));
            documents.add(new DocumentDto("Driving License", claim.getDrivingLicenseUrl(), "Claim document: Driver's License"));
            if (claim.getPhotoUrl() != null) {
                documents.add(new DocumentDto("Accident Photo", claim.getPhotoUrl(), "Claim document: Photo of incident"));
            }
            if (claim.getVideoUrl() != null) {
                documents.add(new DocumentDto("Accident Video", claim.getVideoUrl(), "Claim document: Video of incident"));
            }
        }
        return documents;
    }

     @Transactional
    public Lead createLead(LeadRequest request, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);

        Lead lead = Lead.builder()
                .superagent(superagent)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .notes(request.getNotes())
                .status(request.getStatus())
                .build();
        
        return leadRepository.save(lead);
    }

    @Transactional(readOnly = true)
    public List<Lead> viewLeads(User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        return leadRepository.findBySuperagentOrderByCreatedAtDesc(superagent);
    }

    @Transactional
    public Lead updateLead(Long leadId, LeadRequest request, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + leadId));

        // Security Check: Ensure the superagent owns this lead
        if (!lead.getSuperagent().getId().equals(superagent.getId())) {
            throw new SecurityException("You are not authorized to update this lead.");
        }

        lead.setCustomerName(request.getCustomerName());
        lead.setCustomerPhone(request.getCustomerPhone());
        lead.setCustomerEmail(request.getCustomerEmail());
        lead.setNotes(request.getNotes());
        lead.setStatus(request.getStatus());
        lead.setUpdatedAt(LocalDateTime.now());
        
        return leadRepository.save(lead);
    }

    public void deleteLead(Long leadId, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + leadId));

        if (!lead.getSuperagent().getId().equals(superagent.getId())) {
            throw new SecurityException("You are not authorized to delete this lead.");
        }
        
        leadRepository.delete(lead);
    }

    // --- Claim Management ---
    @Transactional(readOnly = true)
    public List<Claim> viewClaims(User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        return claimRepository.findClaimsBySuperagent(superagent);
    }

    @Transactional
    public Claim updateClaimStatus(Long claimId, UpdateClaimStatusRequest request, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with ID: " + claimId));

        if (!claim.getPolicy().getAgent().getSuperagent().getId().equals(superagent.getId())) {
            throw new SecurityException("You are not authorized to manage this claim.");
        }

        claim.setStatus(request.getStatus());
        claim.setUpdatedAt(LocalDateTime.now());
        Claim updatedClaim = claimRepository.save(claim);

        User agentUser = claim.getPolicy().getAgent().getUser();
        String message = String.format("Your claim (ID: %d) has been updated to status: %s.",
                updatedClaim.getId(), updatedClaim.getStatus());
        notificationService.createNotification(superagent.getUser(), agentUser, message);
        return updatedClaim;
    }

    // --- Payout Management ---
    @Transactional(readOnly = true)
    public List<Transaction> viewPendingWithdrawals(User currentUser) {
        return transactionRepository.findAgentTransactionsByStatus(TransactionStatus.PENDING);
    }

    @Transactional
    public Transaction approveWithdrawal(ApproveWithdrawalDto request, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        Wallet superagentWallet = walletRepository.findByUser(superagent.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for superagent: " + currentUser.getFullName()));
        
        Transaction withdrawalTx = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal transaction not found with ID: " + request.getTransactionId()));

        if (withdrawalTx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("This transaction is not in a pending state.");
        }

        Agent agent = withdrawalTx.getWallet().getUser().getAgentProfile();
        if (agent == null || !agent.getSuperagent().getId().equals(superagent.getId())) {
             throw new SecurityException("You are not authorized to approve this withdrawal.");
        }
        
        BigDecimal amount = withdrawalTx.getAmount();

        Transaction debitTx = Transaction.builder()
                .wallet(superagentWallet)
                .amount(amount.negate())
                .transactionType(TransactionType.PAYOUT_DEBIT)
                .status(TransactionStatus.COMPLETED)
                .build();
        transactionRepository.save(debitTx);
        
        withdrawalTx.setStatus(TransactionStatus.COMPLETED);
        withdrawalTx.setTransactionType(TransactionType.WITHDRAWAL_COMPLETED);
        
        String message = String.format("Your withdrawal request for KES %.2f has been approved and processed.", amount);
        notificationService.createNotification(currentUser, agent.getUser(), message);
        
        return transactionRepository.save(withdrawalTx);
    }
}