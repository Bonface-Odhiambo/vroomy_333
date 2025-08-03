package com.insuranceplatform.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insuranceplatform.backend.dto.CompanyRequest;
import com.insuranceplatform.backend.dto.CreatePolicyRequest;
import com.insuranceplatform.backend.dto.ProductRequest;
import com.insuranceplatform.backend.dto.RegisterRequest;
import com.insuranceplatform.backend.dto.TaxRateRequest;
import com.insuranceplatform.backend.entity.InsuranceCompany;
import com.insuranceplatform.backend.entity.Product;
import com.insuranceplatform.backend.enums.UserRole;
import com.insuranceplatform.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // Starts the full application context
@AutoConfigureMockMvc // Gives us a tool (MockMvc) to make fake HTTP requests
public class PolicyCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Our tool for making API calls

    @Autowired
    private ObjectMapper objectMapper; // For converting Java objects to JSON strings

    @Autowired
    private AuthService authService;

    private String adminToken;
    private String superagentToken;
    private String agentToken;

    @BeforeEach
    void setUp() {
        // Create users and get their tokens before each test
        adminToken = authService.register(
            new RegisterRequest("Admin User", "admin.test@example.com", "111", "pass", UserRole.ADMIN, null, null, null)
        ).getToken();

        RegisterRequest superagentReg = new RegisterRequest("Superagent User", "super.test@example.com", "222", "pass", UserRole.SUPERAGENT, "IRA123", "KRA123", null);
        superagentToken = authService.register(superagentReg).getToken();

        RegisterRequest agentReg = new RegisterRequest("Agent User", "agent.test@example.com", "333", "pass", UserRole.AGENT, null, null, 1L);
        agentToken = authService.register(agentReg).getToken();
    }

    @Test
    void testFullPolicyCreationFlow() throws Exception {
        // --- 1. Admin sets up the system ---
        // Admin sets tax rate
        TaxRateRequest taxRequest = new TaxRateRequest();
        taxRequest.setTaxRate(new BigDecimal("16.00"));
        mockMvc.perform(post("/api/v1/admin/config/tax")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taxRequest)))
                .andExpect(status().isOk());

        // Admin creates an insurance company
        CompanyRequest companyRequest = new CompanyRequest();
        companyRequest.setName("Test Insurance Co");
        companyRequest.setIraNumber("COMP-IRA-001");
        MvcResult companyResult = mockMvc.perform(post("/api/v1/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        InsuranceCompany createdCompany = objectMapper.readValue(companyResult.getResponse().getContentAsString(), InsuranceCompany.class);

        // --- 2. Superagent creates a product ---
        ProductRequest productRequest = new ProductRequest();
        productRequest.setName("Motor Private");
        productRequest.setRate(new BigDecimal("5.00")); // 5%
        productRequest.setCompanyId(createdCompany.getId());
        MvcResult productResult = mockMvc.perform(post("/api/v1/superagents/products")
                        .header("Authorization", "Bearer " + superagentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Product createdProduct = objectMapper.readValue(productResult.getResponse().getContentAsString(), Product.class);

        // --- 3. Agent creates a policy ---
        CreatePolicyRequest policyRequest = new CreatePolicyRequest();
        policyRequest.setClientFullName("John Doe");
        policyRequest.setClientIdentifier("ID12345");
        policyRequest.setProductId(createdProduct.getId());
        policyRequest.setInsuredValue(new BigDecimal("1000000")); // Car value of 1 million

        mockMvc.perform(post("/api/v1/agents/policies")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(58000.00)); // Premium=50k, Tax=8k
    }
}