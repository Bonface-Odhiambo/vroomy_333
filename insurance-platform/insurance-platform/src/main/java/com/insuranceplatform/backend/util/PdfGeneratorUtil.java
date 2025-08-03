package com.insuranceplatform.backend.util;

import com.insuranceplatform.backend.entity.Policy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PdfGeneratorUtil {

    /**
     * Mocks the generation of a PDF certificate for a policy.
     * In a real application, this would use a library like iText or Apache PDFBox
     * to create a real PDF file from a template.
     *
     * @param policy The policy for which to generate a certificate.
     * @return A fake path or URL to the "generated" PDF.
     */
    public String generatePolicyCertificate(Policy policy) {
        String fileName = String.format("cert_%d_%s.pdf", policy.getId(), policy.getClient().getFullName().replaceAll("\\s+", "_"));
        String fakeStoragePath = "/certificates/" + fileName;

        log.info("MOCK PDF GENERATION: Creating certificate for policy ID {}.", policy.getId());
        log.info(" - Client: {}", policy.getClient().getFullName());
        log.info(" - Product: {}", policy.getProduct().getName());
        log.info(" - Total Amount: {}", policy.getTotalAmount());
        log.info(" - Certificate would be saved to: {}", fakeStoragePath);

        // TODO: Replace this with a real PDF generation library like iText.
        
        return fakeStoragePath;
    }
}