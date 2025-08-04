package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.ProductRequest;
import com.insuranceplatform.backend.dto.UpdateAgentStatusRequest;
import com.insuranceplatform.backend.dto.UpdateClaimStatusRequest;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.service.SuperagentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.insuranceplatform.backend.dto.DocumentDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.insuranceplatform.backend.dto.ApproveWithdrawalDto;
import com.insuranceplatform.backend.dto.LeadRequest;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/superagents")
@RequiredArgsConstructor
public class SuperagentController {

    private final SuperagentService superagentService;

    // --- Product Management ---
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(
            @RequestBody ProductRequest request,
            @AuthenticationPrincipal User currentUser) {
        Product createdProduct = superagentService.createProduct(request, currentUser);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }
    @GetMapping("/payouts/pending")
    public ResponseEntity<List<Transaction>> getPendingWithdrawals(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(superagentService.viewPendingWithdrawals(currentUser));
    }
    @GetMapping("/policies/{policyId}/documents")
    public ResponseEntity<List<DocumentDto>> viewPolicyDocuments(
            @PathVariable Long policyId,
            @AuthenticationPrincipal User currentUser) {
        List<DocumentDto> documents = superagentService.viewPolicyDocuments(policyId, currentUser);
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/leads")
    public ResponseEntity<Lead> createLead(
            @Valid @RequestBody LeadRequest request,
            @AuthenticationPrincipal User currentUser) {
        Lead newLead = superagentService.createLead(request, currentUser);
        return new ResponseEntity<>(newLead, HttpStatus.CREATED);
    }

    @GetMapping("/leads")
    public ResponseEntity<List<Lead>> viewLeads(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(superagentService.viewLeads(currentUser));
    }

    @PutMapping("/leads/{leadId}")
    public ResponseEntity<Lead> updateLead(
            @PathVariable Long leadId,
            @Valid @RequestBody LeadRequest request,
            @AuthenticationPrincipal User currentUser) {
        Lead updatedLead = superagentService.updateLead(leadId, request, currentUser);
        return ResponseEntity.ok(updatedLead);
    }

    @DeleteMapping("/leads/{leadId}")
    public ResponseEntity<Void> deleteLead(
            @PathVariable Long leadId,
            @AuthenticationPrincipal User currentUser) {
        superagentService.deleteLead(leadId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/certificate-stock")
    public ResponseEntity<List<CertificateStock>> viewMyCertificateStock(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(superagentService.viewCertificateStock(currentUser));
    }

    @PostMapping("/payouts/approve")
    public ResponseEntity<Transaction> approveWithdrawal(
            @RequestBody ApproveWithdrawalDto request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(superagentService.approveWithdrawal(request, currentUser));
    }

     @GetMapping("/agent-renewals")
    public ResponseEntity<List<Policy>> getAgentRenewals(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "30") int days) { // Default to 30 days
        List<Policy> policies = superagentService.viewAgentRenewals(currentUser, days);
        return ResponseEntity.ok(policies);
    }

    // --- Agent Management ---
    @GetMapping("/my-agents")
    public ResponseEntity<List<Agent>> viewMyAgents(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(superagentService.viewMyAgents(currentUser));
    }

    @PatchMapping("/my-agents/{agentUserId}/status")
    public ResponseEntity<User> updateAgentStatus(
            @PathVariable Long agentUserId,
            @RequestBody UpdateAgentStatusRequest request,
            @AuthenticationPrincipal User currentUser) {
        User updatedUser = superagentService.updateAgentStatus(agentUserId, request, currentUser);
        return ResponseEntity.ok(updatedUser);
    }

    // --- Reporting ---
    @GetMapping("/agent-transactions")
    public ResponseEntity<List<Policy>> viewAgentTransactions(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(superagentService.viewAgentTransactions(currentUser));
    }

    // --- Claim Management ---
    @GetMapping("/claims")
    public ResponseEntity<List<Claim>> viewClaims(@AuthenticationPrincipal User currentUser) {
        List<Claim> claims = superagentService.viewClaims(currentUser);
        return ResponseEntity.ok(claims);
    }

    @PatchMapping("/claims/{claimId}/status")
    public ResponseEntity<Claim> updateClaimStatus(
            @PathVariable Long claimId,
            @RequestBody UpdateClaimStatusRequest request,
            @AuthenticationPrincipal User currentUser) {
        Claim updatedClaim = superagentService.updateClaimStatus(claimId, request, currentUser);
        return ResponseEntity.ok(updatedClaim);
    }
}