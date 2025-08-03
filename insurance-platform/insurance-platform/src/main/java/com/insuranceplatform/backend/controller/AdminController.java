package com.insuranceplatform.backend.controller;

import com.insuranceplatform.backend.dto.CompanyRequest;
import com.insuranceplatform.backend.dto.DashboardMetricsDto;
import com.insuranceplatform.backend.dto.TaxRateRequest;
import com.insuranceplatform.backend.dto.UserStatusRequest;
import com.insuranceplatform.backend.entity.ApiKey;
import com.insuranceplatform.backend.entity.GlobalConfig;
import com.insuranceplatform.backend.entity.InsuranceCompany;
import com.insuranceplatform.backend.entity.User;
import com.insuranceplatform.backend.service.AdminService;
import com.insuranceplatform.backend.service.ReportingService;
import lombok.RequiredArgsConstructor;
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

    // --- Dashboard & Metrics ---

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics() {
        DashboardMetricsDto metrics = adminService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }

    // --- Insurance Company Management ---

    @PostMapping("/companies")
    public ResponseEntity<InsuranceCompany> createCompany(@RequestBody CompanyRequest request) {
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

    @DeleteMapping("/companies/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        adminService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    // --- User Management ---

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<User> updateUserStatus(@PathVariable Long userId, @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request));
    }

    // --- Global Configuration ---

    @PostMapping("/config/tax")
    public ResponseEntity<GlobalConfig> setTaxRate(@RequestBody TaxRateRequest request) {
        return ResponseEntity.ok(adminService.setTaxRate(request));
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

    @GetMapping("/reports/transactions")
    public ResponseEntity<String> exportTransactions() {
        String csvData = reportingService.generateAgentTransactionsCsv();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN); // Can be MediaType.TEXT_CSV
        headers.setContentDispositionFormData("attachment", "transactions.csv");

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }
}