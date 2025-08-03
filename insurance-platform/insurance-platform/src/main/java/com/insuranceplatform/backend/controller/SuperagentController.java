package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.ProductRequest;
import com.insuranceplatform.backend.entity.Product;
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.service.SuperagentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/superagents")
@RequiredArgsConstructor
public class SuperagentController {

    private final SuperagentService superagentService;

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(
            @RequestBody ProductRequest request,
            @AuthenticationPrincipal User currentUser) { // Injects the logged-in user details
        Product createdProduct = superagentService.createProduct(request, currentUser);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @GetMapping("/my-agents")
    public ResponseEntity<String> viewMyAgents(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(superagentService.viewMyAgents(currentUser));
    }
    
    // Other endpoints for managing agents, viewing reports, etc. will be added here
}