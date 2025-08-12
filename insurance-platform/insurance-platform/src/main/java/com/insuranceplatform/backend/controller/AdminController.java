package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.*; // Assuming new DTOs are in this package
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.service.AdminService;
import com.insuranceplatform.backend.service.ReportingService;
import com.insuranceplatform.backend.service.UserService; // Import the new service
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource; // Import for file streaming
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReportingService reportingService;
    // ADDED: UserService to handle general user retrieval logic
    private final UserService userService;

    // --- Dashboard & Metrics ---

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics() {
        DashboardMetricsDto metrics = adminService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }
    
    @PostMapping("/certificate-stock")
    public ResponseEntity<CertificateStock> addCertificateStock(@Valid @RequestBody AddStockRequest request) {
        CertificateStock updatedStock = adminService.addCertificateStock(request);
        return new ResponseEntity<>(updatedStock, HttpStatus.OK);
    }

    // --- Insurance Company Management (CRUD) ---

    @PostMapping("/companies")
    public ResponseEntity<InsuranceCompany> createCompany(@Valid @RequestBody CompanyRequest request) {
        return new ResponseEntity<>(adminService.createCompany(request), HttpStatus.CREATED);
    }

    @GetMapping("/companies")
    public ResponseEntity<List<InsuranceCompany>> getAllCompanies() {
        return ResponseEntity.ok(adminService.getAllCompanies());
    }

    @GetMapping("/companies/{id}")
    public ResponseEntity<InsuranceCompany> getCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getCompanyById(id));
    }
    
    /**
     * NEW ENDPOINT: Update an existing insurance company's details.
     */
    @PutMapping("/companies/{id}")
    public ResponseEntity<InsuranceCompany> updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        InsuranceCompany updatedCompany = adminService.updateCompany(id, request);
        return ResponseEntity.ok(updatedCompany);
    }

    @DeleteMapping("/companies/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        adminService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    // --- User Management ---

    /**
     * NEW ENDPOINT: Get a list of all users (Agents and Superagents).
     * Supports filtering by role (e.g., /users?role=AGENT).
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers(@RequestParam(required = false) String role) {
        List<UserDto> users = userService.findAllUsers(role);
        return ResponseEntity.ok(users);
    }

    /**
     * NEW ENDPOINT: Get detailed information for a single user by their ID.
     * Responds with a DTO to avoid exposing sensitive data.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId) {
        UserDto user = userService.findUserDtoById(userId);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<User> updateUserStatus(@PathVariable Long userId, @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request));
    }

    /**
     * NEW ENDPOINT: Approve a newly registered Superagent, making their account active.
     * This is a critical step in the onboarding workflow.
     */
    @PatchMapping("/superagents/{superagentId}/approve")
    public ResponseEntity<Void> approveSuperagent(@PathVariable Long superagentId) {
        adminService.approveSuperagent(superagentId);
        return ResponseEntity.ok().build();
    }

    // --- Global Configuration ---
    
    @PostMapping("/config/tax")
    public ResponseEntity<GlobalConfig> setTaxRate(@RequestBody TaxRateRequest request) {
        return ResponseEntity.ok(adminService.setTaxRate(request));
    }
    
    /**
     * NEW ENDPOINT: A more generic way to update any global setting.
     */
    @PutMapping("/config")
    public ResponseEntity<GlobalConfig> updateGlobalConfig(@Valid @RequestBody GlobalConfig config) {
        GlobalConfig updatedConfig = adminService.updateConfig(config);
        return ResponseEntity.ok(updatedConfig);
    }

    @GetMapping("/config")
    public ResponseEntity<GlobalConfig> getGlobalConfig() {
        return ResponseEntity.ok(adminService.getGlobalConfig());
    }

    // --- API Key Management for Data Sharing ---

    @PostMapping("/companies/{companyId}/generate-key")
    public ResponseEntity<ApiKey> generateApiKey(@PathVariable Long companyId) {
        ApiKey newKey = adminService.generateApiKey(companyId);
        return new ResponseEntity<>(newKey, HttpStatus.CREATED);
    }

    // --- Data Reporting & Export ---
    
    /**
     * UPGRADED ENDPOINT: Exports all agent transactions as a downloadable CSV file.
     * This now uses a Resource for efficient streaming, preventing memory issues with large reports.
     */
    @GetMapping(value = "/reports/transactions.csv", produces = "text/csv")
    public ResponseEntity<Resource> exportTransactionsAsCsv() {
        Resource resource = reportingService.generateTransactionsCsvAsResource();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}