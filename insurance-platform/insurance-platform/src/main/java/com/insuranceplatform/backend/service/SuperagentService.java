package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.ProductRequest;
import com.insuranceplatform.backend.entity.InsuranceCompany;
import com.insuranceplatform.backend.entity.Product;
import com.insuranceplatform.backend.entity.Superagent;
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.InsuranceCompanyRepository;
import com.insuranceplatform.backend.repository.ProductRepository;
import com.insuranceplatform.backend.repository.SuperagentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuperagentService {

    private final ProductRepository productRepository;
    private final SuperagentRepository superagentRepository;
    private final InsuranceCompanyRepository companyRepository;

    public Product createProduct(ProductRequest request, User currentUser) {
        // Find the Superagent profile associated with the currently logged-in user
        Superagent superagent = superagentRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Superagent profile not found for the current user."));

        // Find the insurance company the product is for
        InsuranceCompany company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceCompany not found with ID: " + request.getCompanyId()));

        Product product = Product.builder()
                .superagent(superagent)
                .insuranceCompany(company)
                .name(request.getName())
                .rate(request.getRate())
                .build();

        return productRepository.save(product);
    }
    
    // Placeholder for viewing agents - we will build this out later
    public String viewMyAgents(User currentUser) {
        return "Feature to view agents for Superagent " + currentUser.getFullName() + " is coming soon.";
    }

    // Add more methods here later for viewing transactions, renewals, etc.
}