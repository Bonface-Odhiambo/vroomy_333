package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.CreatePolicyRequest;
import com.insuranceplatform.backend.entity.Policy;
import com.insuranceplatform.backend.entity.Product;
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // <-- Add this import

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAvailableProducts(@AuthenticationPrincipal User currentUser) {
        List<Product> products = agentService.viewAvailableProducts(currentUser);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/policies")
    public ResponseEntity<Policy> createPolicy(
            @RequestBody CreatePolicyRequest request,
            @AuthenticationPrincipal User currentUser) {
        Policy newPolicy = agentService.createPolicy(request, currentUser);
        return new ResponseEntity<>(newPolicy, HttpStatus.CREATED);
    }

    @GetMapping("/policies")
    public ResponseEntity<List<Policy>> getMyPolicies(@AuthenticationPrincipal User currentUser) {
        List<Policy> policies = agentService.viewMyPolicies(currentUser);
        return ResponseEntity.ok(policies);
    }

    // --- THIS IS THE CORRECTED ENDPOINT ---
    @PostMapping("/policies/{policyId}/documents")
    public ResponseEntity<Policy> uploadDocument(
            @PathVariable Long policyId,
            @RequestParam("file") MultipartFile file, // Changed from String fileUrl
            @RequestParam String documentType,
            @AuthenticationPrincipal User currentUser) {

        Policy updatedPolicy = agentService.uploadDocument(policyId, file, documentType, currentUser);
        return ResponseEntity.ok(updatedPolicy);
    }
}