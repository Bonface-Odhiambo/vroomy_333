package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.ProductRequest;
import com.insuranceplatform.backend.dto.UpdateAgentStatusRequest; // New
import com.insuranceplatform.backend.dto.UpdateClaimStatusRequest;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.*; // Import all
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.insuranceplatform.backend.dto.ApproveWithdrawalDto;
import com.insuranceplatform.backend.enums.TransactionStatus;
import com.insuranceplatform.backend.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperagentService {

    private final ProductRepository productRepository;
    private final SuperagentRepository superagentRepository;
    private final InsuranceCompanyRepository companyRepository;
    private final ClaimRepository claimRepository;
    private final NotificationService notificationService;
    private final AgentRepository agentRepository; // New
    private final UserRepository userRepository; // New
    private final PolicyRepository policyRepository; // New
     private final WalletRepository walletRepository; // Ensure this is injected
    private final TransactionRepository transactionRepository; // Ensure this is injected

    private Superagent getSuperagentProfile(User currentUser) {
        return superagentRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Superagent profile not found for the current user."));
    }

    public List<Policy> viewAgentRenewals(User currentUser, int daysUntilExpiry) {
        Superagent superagent = getSuperagentProfile(currentUser);
        LocalDateTime expiryCutoffDate = LocalDateTime.now().plusDays(daysUntilExpiry);
        return policyRepository.findByAgent_SuperagentAndExpiryDateBefore(superagent, expiryCutoffDate);
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

    // --- Agent Management ---

    @Transactional(readOnly = true)
    public List<Agent> viewMyAgents(User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        return agentRepository.findBySuperagent(superagent);
    }

    @Transactional
    public User updateAgentStatus(Long agentUserId, UpdateAgentStatusRequest request, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);

        // Find the user profile of the agent to be updated
        User agentUser = userRepository.findById(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent user not found with ID: " + agentUserId));

        // Find the agent-specific profile to perform security check
        Agent agentProfile = agentRepository.findByUser(agentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found for user ID: " + agentUserId));

        // Security Check: Ensure the agent belongs to the logged-in superagent
        if (!agentProfile.getSuperagent().getId().equals(superagent.getId())) {
            throw new SecurityException("You are not authorized to manage this agent.");
        }

        // Update the status and save
        agentUser.setStatus(request.getStatus());
        User updatedUser = userRepository.save(agentUser);

        // Notify the agent
        String message = String.format("Your account status has been updated to %s by your Superagent.", request.getStatus());
        notificationService.createNotification(currentUser, agentUser, message);

        return updatedUser;
    }

    // --- Reporting / Transaction Viewing ---

    @Transactional(readOnly = true)
    public List<Policy> viewAgentTransactions(User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        return policyRepository.findByAgent_SuperagentOrderByCreatedAtDesc(superagent);
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

    // --- Withdrawal Management ---
    @Transactional(readOnly = true)
    public List<Transaction> viewPendingWithdrawals(User currentUser) {
        // Simple implementation: show all pending agent withdrawals.
        // A more complex version would filter for only this superagent's agents.
        return transactionRepository.findAgentTransactionsByStatus(TransactionStatus.PENDING);
    }

    @Transactional
    public Transaction approveWithdrawal(ApproveWithdrawalDto request, User currentUser) {
        Superagent superagent = getSuperagentProfile(currentUser);
        Wallet superagentWallet = walletRepository.findByUser(superagent.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for superagent: " + currentUser.getFullName()));
        
        Transaction withdrawalTx = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal transaction not found with ID: " + request.getTransactionId()));

        // --- VALIDATION CHECKS ---
        if (withdrawalTx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("This transaction is not in a pending state.");
        }
        // Security Check: Make sure this superagent is the one who should approve this
        Agent agent = withdrawalTx.getWallet().getUser().getAgentProfile(); // You'll need to add getAgentProfile() to User entity
        if (!agent.getSuperagent().getId().equals(superagent.getId())) {
             throw new SecurityException("You are not authorized to approve this withdrawal.");
        }
        
        BigDecimal amount = withdrawalTx.getAmount();

        // Simulate debiting the superagent's (hypothetical) real money account.
        // For our system, we can debit a "payout" wallet or just log the debit.
        // Let's create a debit transaction for the superagent.
        Transaction debitTx = Transaction.builder()
                .wallet(superagentWallet)
                .amount(amount.negate()) // Debit is a negative amount
                .transactionType(TransactionType.PAYOUT_DEBIT)
                .status(TransactionStatus.COMPLETED)
                .build();
        transactionRepository.save(debitTx);

        // TODO: In a real app, this is where you call the MPESA B2C API to pay the agent.
        // If the API call is successful, update the withdrawal status.
        
        withdrawalTx.setStatus(TransactionStatus.COMPLETED);
        withdrawalTx.setTransactionType(TransactionType.WITHDRAWAL_COMPLETED);
        
        // Notify the agent
        String message = String.format("Your withdrawal request for KES %.2f has been approved and processed.", amount);
        notificationService.createNotification(currentUser, agent.getUser(), message);
        
        return transactionRepository.save(withdrawalTx);
    }
}