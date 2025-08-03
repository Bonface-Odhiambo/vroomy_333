package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.CreatePolicyRequest;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.enums.PolicyStatus;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileStorageService fileStorageService; // Injected for file handling

    private Agent getAgentProfile(User currentUser) {
        // This is a helper method to avoid duplicating code.
        // It finds the Agent profile for the currently logged-in User.
        return agentRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found for user ID: " + currentUser.getId()));
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
        // TODO: This is a simplified calculation. Real-world logic could be much more complex.
        GlobalConfig config = configRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Global tax configuration is not set."));

        BigDecimal rate = product.getRate(); // This could be a flat fee or a percentage.
        BigDecimal premium = request.getInsuredValue().multiply(rate).divide(BigDecimal.valueOf(100)); // Assuming rate is a percentage
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