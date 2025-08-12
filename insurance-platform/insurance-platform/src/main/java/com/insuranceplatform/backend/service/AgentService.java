package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.*;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.enums.*;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentService {

    // --- All required dependencies injected via constructor ---
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
    private final UserRepository userRepository; // Added
    private final AuthService authService;       // Added

    // --- Profile Management ---

    @Transactional(readOnly = true)
    public Agent getCurrentAgentProfile() {
        User currentUser = authService.getCurrentUser();
        return agentRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found for current user."));
    }

    @Transactional
    public Agent updateCurrentAgentProfile(UpdateProfileRequest request) {
        User currentUser = authService.getCurrentUser();
        Agent agent = getCurrentAgentProfile();

        currentUser.setFullName(request.fullName());
        currentUser.setEmail(request.email());
        userRepository.save(currentUser);

        return agentRepository.save(agent);
    }

    // --- Wallet & Transaction Management ---
    
    @Transactional(readOnly = true)
    public WalletDto getWalletDetailsForCurrentAgent() {
        User currentUser = authService.getCurrentUser();
        Wallet wallet = walletRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for current user."));
        List<Transaction> transactions = transactionRepository.findByWalletOrderByTimestampDesc(wallet);
        return new WalletDto(wallet.getBalance(), transactions);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsForCurrentAgent() {
        User currentUser = authService.getCurrentUser();
        Wallet wallet = walletRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for current user."));
        return transactionRepository.findByWalletOrderByTimestampDesc(wallet);
    }

    @Transactional
    public Transaction requestWithdrawal(WithdrawalRequestDto request) {
        Agent agent = getCurrentAgentProfile();
        Wallet agentWallet = walletRepository.findByUser(agent.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for agent: " + agent.getUser().getFullName()));

        if (agentWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance for this withdrawal request.");
        }

        agentWallet.setBalance(agentWallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(agentWallet);

        Transaction transaction = Transaction.builder()
                .wallet(agentWallet)
                .amount(request.getAmount())
                .transactionType(TransactionType.WITHDRAWAL_REQUEST)
                .status(TransactionStatus.PENDING)
                .build();
        return transactionRepository.save(transaction);
    }
    
    // --- Product & Policy Management ---

    @Transactional(readOnly = true)
    public List<Product> viewAvailableProducts() {
        Agent agent = getCurrentAgentProfile();
        // OPTIMIZED: Use a direct repository query for better performance.
        return productRepository.findBySuperagent(agent.getSuperagent());
    }

    @Transactional(readOnly = true)
    public List<Policy> viewMyPolicies() {
        Agent agent = getCurrentAgentProfile();
        return policyRepository.findByAgent(agent);
    }
    
    @Transactional(readOnly = true)
    public Policy getPolicyDetailsForCurrentAgent(Long policyId) {
        Agent agent = getCurrentAgentProfile();
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + policyId));
        if (!policy.getAgent().getId().equals(agent.getId())) {
            throw new SecurityException("You are not authorized to view this policy.");
        }
        return policy;
    }

    @Transactional
    public Policy createPolicy(CreatePolicyRequest request) {
        Agent agent = getCurrentAgentProfile();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        if (!product.getSuperagent().getId().equals(agent.getSuperagent().getId())) {
            throw new SecurityException("Agent is not authorized to sell this product.");
        }

        GlobalConfig config = configRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Global tax configuration is not set."));

        BigDecimal premium = calculatePremium(product, request.getInsuredValue());
        BigDecimal tax = premium.multiply(config.getTaxRate()).divide(BigDecimal.valueOf(100));
        BigDecimal total = premium.add(tax);

        Client client = clientRepository.save(Client.builder().agent(agent).fullName(request.getClientFullName()).build());
        Policy policy = Policy.builder()
                .agent(agent).client(client).product(product)
                .premiumAmount(premium).taxAmount(tax).totalAmount(total)
                .status(PolicyStatus.PENDING_PAYMENT).build();
        return policyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public List<Policy> viewRenewals(int daysUntilExpiry) {
        Agent agent = getCurrentAgentProfile();
        LocalDateTime expiryCutoffDate = LocalDateTime.now().plusDays(daysUntilExpiry);
        return policyRepository.findByAgentAndExpiryDateBefore(agent, expiryCutoffDate);
    }
    
    // --- Document & Certificate Management ---

    @Transactional
    public Policy uploadDocument(Long policyId, MultipartFile file, String documentType) {
        // We call getPolicyDetailsForCurrentAgent, which handles getting the current
        // agent and ensuring they own the policy. No need to get the agent again here.
        Policy policy = getPolicyDetailsForCurrentAgent(policyId);

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

    public Resource generateAndGetCertificate(Long policyId) {
        Policy policy = getPolicyDetailsForCurrentAgent(policyId);
        String certificatePathString = policy.getCertificateUrl();

        if (certificatePathString == null || certificatePathString.isBlank()) {
            throw new ResourceNotFoundException("Certificate not generated or found for this policy.");
        }
        
        try {
            // This path needs to be configured properly in your application.
            Path filePath = Paths.get("uploads/certificates/").resolve(certificatePathString).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Certificate file not found or is not readable: " + certificatePathString);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Error reading certificate file path: " + certificatePathString, e);
        }
    }

    // --- Claim Management ---

    @Transactional(readOnly = true)
    public List<Claim> findClaimsByCurrentAgent() {
        Agent agent = getCurrentAgentProfile();
        return claimRepository.findByPolicy_Agent(agent);
    }

    @Transactional(readOnly = true)
    public Claim findClaimByIdForCurrentAgent(Long claimId) {
        Agent agent = getCurrentAgentProfile();
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with ID: " + claimId));
        if (!claim.getPolicy().getAgent().getId().equals(agent.getId())) {
            throw new SecurityException("You are not authorized to view this claim.");
        }
        return claim;
    }

    @Transactional
    public Claim raiseClaim(RaiseClaimRequest request) {
        Agent agent = getCurrentAgentProfile();
        Policy policy = getPolicyDetailsForCurrentAgent(request.getPolicyId());

        Claim newClaim = Claim.builder()
                .policy(policy)
                .description(request.getDescription())
                .status(ClaimStatus.RAISED)
                .policeAbstractUrl("PENDING_UPLOAD")
                .drivingLicenseUrl("PENDING_UPLOAD")
                .logbookUrl("PENDING_UPLOAD").build();
        Claim savedClaim = claimRepository.save(newClaim);

        String message = String.format("Agent %s has raised a new claim (ID: %d) for policy %d.",
                agent.getUser().getFullName(), savedClaim.getId(), policy.getId());
        notificationService.createNotification(agent.getUser(), agent.getSuperagent().getUser(), message);

        return savedClaim;
    }

    // --- Private Helper Methods ---

    private BigDecimal calculatePremium(Product product, BigDecimal insuredValue) {
        switch (product.getCalculationType()) {
            case PERCENTAGE_OF_VALUE:
                if (insuredValue == null || insuredValue.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Insured value must be provided for percentage-based products.");
                }
                return insuredValue.multiply(product.getRate()).divide(BigDecimal.valueOf(100));
            case FLAT_RATE:
                return product.getRate();
            default:
                throw new IllegalStateException("Unknown calculation type: " + product.getCalculationType());
        }
    }
}