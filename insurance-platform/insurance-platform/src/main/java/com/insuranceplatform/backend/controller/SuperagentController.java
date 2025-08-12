package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.*;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.service.SuperagentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/superagents")
@RequiredArgsConstructor
public class SuperagentController {

    private final SuperagentService superagentService;

    // --- Profile Management ---

    @GetMapping("/me")
    public ResponseEntity<Superagent> getMyProfile() {
        return ResponseEntity.ok(superagentService.getCurrentSuperagentProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<Superagent> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Superagent updatedProfile = superagentService.updateCurrentSuperagentProfile(request);
        return ResponseEntity.ok(updatedProfile);
    }

    // --- Dashboard ---

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics() {
        return ResponseEntity.ok(superagentService.getDashboardMetrics());
    }

    // --- Product Management (Full CRUD) ---

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product createdProduct = superagentService.createProduct(request);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getMyProducts() {
        return ResponseEntity.ok(superagentService.viewMyProducts());
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductRequest request) {
        Product updatedProduct = superagentService.updateProduct(productId, request);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        superagentService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // --- Agent Management ---

    @PostMapping("/my-agents")
    public ResponseEntity<Agent> createAgent(@Valid @RequestBody CreateAgentRequest request) {
        Agent newAgent = superagentService.createAgent(request);
        return new ResponseEntity<>(newAgent, HttpStatus.CREATED);
    }

    @GetMapping("/my-agents")
    public ResponseEntity<List<Agent>> viewMyAgents() {
        return ResponseEntity.ok(superagentService.viewMyAgents());
    }

    @PatchMapping("/my-agents/{agentUserId}/status")
    public ResponseEntity<User> updateAgentStatus(@PathVariable Long agentUserId, @RequestBody UpdateAgentStatusRequest request) {
        User updatedUser = superagentService.updateAgentStatus(agentUserId, request);
        return ResponseEntity.ok(updatedUser);
    }
    
    // --- Lead Management ---
    
    @PostMapping("/leads")
    public ResponseEntity<Lead> createLead(@Valid @RequestBody LeadRequest request) {
        Lead newLead = superagentService.createLead(request);
        return new ResponseEntity<>(newLead, HttpStatus.CREATED);
    }

    @GetMapping("/leads")
    public ResponseEntity<List<Lead>> viewLeads() {
        return ResponseEntity.ok(superagentService.viewLeads());
    }

    @PutMapping("/leads/{leadId}")
    public ResponseEntity<Lead> updateLead(@PathVariable Long leadId, @Valid @RequestBody LeadRequest request) {
        Lead updatedLead = superagentService.updateLead(leadId, request);
        return ResponseEntity.ok(updatedLead);
    }

    @DeleteMapping("/leads/{leadId}")
    public ResponseEntity<Void> deleteLead(@PathVariable Long leadId) {
        superagentService.deleteLead(leadId);
        return ResponseEntity.noContent().build();
    }

    // --- Claim Management ---

    @GetMapping("/claims")
    public ResponseEntity<List<Claim>> viewClaims() {
        List<Claim> claims = superagentService.viewClaims();
        return ResponseEntity.ok(claims);
    }

    @PatchMapping("/claims/{claimId}/status")
    public ResponseEntity<Claim> updateClaimStatus(@PathVariable Long claimId, @RequestBody UpdateClaimStatusRequest request) {
        Claim updatedClaim = superagentService.updateClaimStatus(claimId, request);
        return ResponseEntity.ok(updatedClaim);
    }

    // --- Payouts, Reporting & Renewals ---

    @GetMapping("/payouts/pending")
    public ResponseEntity<List<Transaction>> getPendingWithdrawals() {
        return ResponseEntity.ok(superagentService.viewPendingWithdrawals());
    }

    @PostMapping("/payouts/approve")
    public ResponseEntity<Transaction> approveWithdrawal(@RequestBody ApproveWithdrawalDto request) {
        return ResponseEntity.ok(superagentService.approveWithdrawal(request));
    }

    @GetMapping("/agent-transactions")
    public ResponseEntity<List<Policy>> viewAgentTransactions() {
        return ResponseEntity.ok(superagentService.viewAgentTransactions());
    }

    @GetMapping("/agent-renewals")
    public ResponseEntity<List<Policy>> getAgentRenewals(@RequestParam(defaultValue = "30") int days) {
        List<Policy> policies = superagentService.viewAgentRenewals(days);
        return ResponseEntity.ok(policies);
    }
    
    @GetMapping("/certificate-stock")
    public ResponseEntity<List<CertificateStock>> viewMyCertificateStock() {
        return ResponseEntity.ok(superagentService.viewCertificateStock());
    }

    // --- Document Management ---

    @GetMapping("/policies/{policyId}/documents")
    public ResponseEntity<List<DocumentDto>> viewPolicyDocuments(@PathVariable Long policyId) {
        List<DocumentDto> documents = superagentService.viewPolicyDocuments(policyId);
        return ResponseEntity.ok(documents);
    }
}