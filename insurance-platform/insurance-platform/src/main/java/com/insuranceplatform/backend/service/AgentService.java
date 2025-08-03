package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.CreatePolicyRequest;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.enums.PolicyStatus;
import com.insuranceplatform.backend.dto.RaiseClaimRequest; // New
import com.insuranceplatform.backend.enums.ClaimStatus; // New
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import com.insuranceplatform.backend.dto.WithdrawalRequestDto;
import com.insuranceplatform.backend.enums.TransactionStatus;
import com.insuranceplatform.backend.enums.TransactionType;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final PolicyRepository policyRepository;
    private final GlobalConfigRepository configRepository;
    private final FileStorageService fileStorageService; 
    private final ClaimRepository claimRepository;
    private final NotificationService notificationService; 
    private final WalletRepository walletRepository; 
    private final TransactionRepository transactionRepository; 

    private Agent getAgentProfile(User currentUser) {
        // This is a helper method to avoid duplicating code.
        // It finds the Agent profile for the currently logged-in User.
        return agentRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found for user ID: " + currentUser.getId()));
    }

    public List<Policy> viewRenewals(User currentUser, int daysUntilExpiry) {
        Agent agent = getAgentProfile(currentUser);
        LocalDateTime expiryCutoffDate = LocalDateTime.now().plusDays(daysUntilExpiry);
        return policyRepository.findByAgentAndExpiryDateBefore(agent, expiryCutoffDate);
    }

    public List<Product> viewAvailableProducts(User currentUser) {
        Agent agent = getAgentProfile(currentUser);
        Superagent superagent = agent.getSuperagent();
        // This logic is slightly simplified. A more robust solution might query products directly.
        // For now, this demonstrates the relationship traversal.
        return productRepository.findAll().stream()
                .filter(p -> p.getSuperagent().getId().equals(superagent.getId()))
                .toList();
    }

    @Transactional
    public Policy createPolicy(CreatePolicyRequest request, User currentUser) {
        Agent agent = getAgentProfile(currentUser);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        // Security Check: Ensure the agent is selling a product owned by their superagent.
        if (!product.getSuperagent().getId().equals(agent.getSuperagent().getId())) {
            throw new SecurityException("Agent is not authorized to sell this product.");
        }

        // --- Calculate Policy Cost ---
         GlobalConfig config = configRepository.findById(1L)
            .orElseThrow(() -> new IllegalStateException("Global tax configuration is not set."));

    BigDecimal premium;
    switch (product.getCalculationType()) {
        case PERCENTAGE_OF_VALUE:
            if (request.getInsuredValue() == null || request.getInsuredValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Insured value must be provided for percentage-based products.");
            }
            // Premium = (Insured Value * Rate) / 100
            premium = request.getInsuredValue().multiply(product.getRate()).divide(BigDecimal.valueOf(100));
            break;
        case FLAT_RATE:
            // Premium is simply the rate itself
            premium = product.getRate();
            break;
        default:
            throw new IllegalStateException("Unknown calculation type: " + product.getCalculationType());
    }

    BigDecimal tax = premium.multiply(config.getTaxRate()).divide(BigDecimal.valueOf(100));
    BigDecimal total = premium.add(tax);
        // --- End Calculation ---

        // Create the client
        Client client = Client.builder()
                .agent(agent)
                .fullName(request.getClientFullName())
                .build();
        Client savedClient = clientRepository.save(client);

        // Create the policy
        Policy policy = Policy.builder()
                .agent(agent)
                .client(savedClient)
                .product(product)
                .premiumAmount(premium)
                .taxAmount(tax)
                .totalAmount(total)
                .status(PolicyStatus.PENDING_PAYMENT)
                .build();

        return policyRepository.save(policy);
    }

    public List<Policy> viewMyPolicies(User currentUser) {
        Agent agent = getAgentProfile(currentUser);
        return policyRepository.findByAgent(agent);
    }

    @Transactional
    public Transaction requestWithdrawal(WithdrawalRequestDto request, User currentUser) {
        Agent agent = getAgentProfile(currentUser);
        Wallet agentWallet = walletRepository.findByUser(agent.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for agent: " + currentUser.getFullName()));

        BigDecimal amount = request.getAmount();

        // Check if the agent has sufficient balance
        if (agentWallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance for this withdrawal request.");
        }

        // Deduct the amount from the balance immediately to prevent double-spending
        agentWallet.setBalance(agentWallet.getBalance().subtract(amount));
        walletRepository.save(agentWallet);

        // Create a pending withdrawal transaction
        Transaction transaction = Transaction.builder()
                .wallet(agentWallet)
                .amount(amount)
                .transactionType(TransactionType.WITHDRAWAL_REQUEST)
                .status(TransactionStatus.PENDING)
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Claim raiseClaim(RaiseClaimRequest request, User currentUser) {
        Agent agent = getAgentProfile(currentUser);
        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + request.getPolicyId()));

        // Security check: ensure agent owns the policy
        if (!policy.getAgent().getId().equals(agent.getId())) {
            throw new SecurityException("You are not authorized to raise a claim for this policy.");
        }

        // Create the initial claim object. The file URLs will be added separately.
        Claim newClaim = Claim.builder()
                .policy(policy)
                .description(request.getDescription())
                .status(ClaimStatus.RAISED) // Initial status
                // Set placeholder URLs - these MUST be updated by file uploads
                .policeAbstractUrl("PENDING_UPLOAD")
                .drivingLicenseUrl("PENDING_UPLOAD")
                .logbookUrl("PENDING_UPLOAD")
                .build();
        
        Claim savedClaim = claimRepository.save(newClaim);

        // Notify the superagent
        Superagent superagent = agent.getSuperagent();
        String message = String.format("Agent %s has raised a new claim (ID: %d) for policy %d.",
                agent.getUser().getFullName(), savedClaim.getId(), policy.getId());
        notificationService.createNotification(agent.getUser(), superagent.getUser(), message);

        return savedClaim;
    }

    @Transactional
    public Policy uploadDocument(Long policyId, MultipartFile file, String documentType, User currentUser) {
        Agent agent = getAgentProfile(currentUser);
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + policyId));

        // Security check: ensure the agent owns the policy they're modifying
        if (!policy.getAgent().getId().equals(agent.getId())) {
            throw new SecurityException("You are not authorized to upload documents for this policy.");
        }

        String fileUrl;
        if ("LOGBOOK".equalsIgnoreCase(documentType)) {
            fileUrl = fileStorageService.storeFile(file, "documents");
            policy.setLogbookFileUrl(fileUrl);
        } else if ("ID_KRA".equalsIgnoreCase(documentType)) {
            fileUrl = fileStorageService.storeFile(file, "documents");
            policy.getClient().setIdFileUrl(fileUrl);
            clientRepository.save(policy.getClient()); // Save the updated client
        } else {
            throw new IllegalArgumentException("Invalid document type specified: " + documentType);
        }

        return policyRepository.save(policy);
    }
}