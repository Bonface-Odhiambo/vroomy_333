package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.*;
import com.insuranceplatform.backend.entity.Agent;
import com.insuranceplatform.backend.entity.Claim;
import com.insuranceplatform.backend.entity.Policy;
import com.insuranceplatform.backend.entity.Product;
import com.insuranceplatform.backend.entity.Transaction;
import com.insuranceplatform.backend.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    // --- Profile Management ---

    @GetMapping("/me")
    public ResponseEntity<Agent> getMyProfile() {
        return ResponseEntity.ok(agentService.getCurrentAgentProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<Agent> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(agentService.updateCurrentAgentProfile(request));
    }

    // --- Wallet & Transaction Management ---

    @GetMapping("/wallet")
    public ResponseEntity<WalletDto> getMyWallet() {
        return ResponseEntity.ok(agentService.getWalletDetailsForCurrentAgent());
    }

    @PostMapping("/wallet/withdraw")
    public ResponseEntity<Transaction> requestWithdrawal(@Valid @RequestBody WithdrawalRequestDto request) {
        Transaction transaction = agentService.requestWithdrawal(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getMyTransactions() {
        return ResponseEntity.ok(agentService.getTransactionsForCurrentAgent());
    }
    
    // --- Product, Policy & Renewal Management ---

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAvailableProducts() {
        return ResponseEntity.ok(agentService.viewAvailableProducts());
    }

    @PostMapping("/policies")
    public ResponseEntity<Policy> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        Policy newPolicy = agentService.createPolicy(request);
        return new ResponseEntity<>(newPolicy, HttpStatus.CREATED);
    }

    @GetMapping("/policies")
    public ResponseEntity<List<Policy>> getMyPolicies() {
        return ResponseEntity.ok(agentService.viewMyPolicies());
    }
    
    @GetMapping("/policies/{policyId}")
    public ResponseEntity<Policy> getMyPolicyDetails(@PathVariable Long policyId) {
        return ResponseEntity.ok(agentService.getPolicyDetailsForCurrentAgent(policyId));
    }

    @GetMapping("/policies/renewals")
    public ResponseEntity<List<Policy>> getMyRenewals(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(agentService.viewRenewals(days));
    }
    
    // --- Document & Certificate Management ---

    @PostMapping("/policies/{policyId}/documents")
    public ResponseEntity<Policy> uploadDocument(
            @PathVariable Long policyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam String documentType) {
        Policy updatedPolicy = agentService.uploadDocument(policyId, file, documentType);
        return ResponseEntity.ok(updatedPolicy);
    }
    
    @GetMapping("/policies/{policyId}/certificate")
    public ResponseEntity<Resource> downloadPolicyCertificate(@PathVariable Long policyId) {
        Resource resource = agentService.generateAndGetCertificate(policyId);
        String filename = "policy_certificate_" + policyId + ".pdf";
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    // --- Claim Management ---

    @PostMapping("/claims")
    public ResponseEntity<Claim> raiseClaim(@Valid @RequestBody RaiseClaimRequest request) {
        Claim newClaim = agentService.raiseClaim(request);
        return new ResponseEntity<>(newClaim, HttpStatus.CREATED);
    }
    
    @GetMapping("/claims")
    public ResponseEntity<List<Claim>> getMyClaims() {
        return ResponseEntity.ok(agentService.findClaimsByCurrentAgent());
    }

    @GetMapping("/claims/{claimId}")
    public ResponseEntity<Claim> getMyClaimDetails(@PathVariable Long claimId) {
        return ResponseEntity.ok(agentService.findClaimByIdForCurrentAgent(claimId));
    }
}