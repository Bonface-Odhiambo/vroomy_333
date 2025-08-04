package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.MpesaCallbackRequest;
import com.insuranceplatform.backend.dto.PaybillDetailsDto;
import com.insuranceplatform.backend.dto.StkPushRequest;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.enums.PolicyStatus;
import com.insuranceplatform.backend.enums.TransactionStatus;
import com.insuranceplatform.backend.enums.TransactionType;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.exception.StkPushException;
import com.insuranceplatform.backend.repository.*;
import com.insuranceplatform.backend.util.PdfGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaService {

    private final PolicyRepository policyRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CertificateRepository certificateRepository;
    private final NotificationService notificationService;
    private final PdfGeneratorUtil pdfGeneratorUtil;
    private final CertificateStockRepository certificateStockRepository;

    public String initiateStkPush(StkPushRequest request) {
        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + request.getPolicyId()));

        log.info("Simulating STK Push to phone {} for policy {} with amount {}",
                request.getPhoneNumber(), policy.getId(), policy.getTotalAmount());

        if ("0700000000".equals(request.getPhoneNumber())) {
            log.error("MOCK STK PUSH FAILED: Phone number {} is blacklisted for testing.", request.getPhoneNumber());
            throw new StkPushException("Failed to initiate STK Push. Please try again later.");
        }

        return "STK Push initiated successfully (simulation).";
    }

    public PaybillDetailsDto getPaybillDetails(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + policyId));

        Superagent superagent = policy.getAgent().getSuperagent();

        if (superagent.getPaybillNumber() == null || superagent.getPaybillNumber().isBlank()) {
            throw new IllegalStateException("Superagent has not configured their Paybill number.");
        }

        String accountNumber = "VROOMY-" + policy.getId();

        return PaybillDetailsDto.builder()
                .paybillNumber(superagent.getPaybillNumber())
                .accountNumber(accountNumber)
                .amount(policy.getTotalAmount())
                .instructions("Go to your M-PESA menu, select Lipa na M-PESA, choose Pay Bill and enter the details above.")
                .build();
    }

    @Transactional
    public void processMpesaCallback(MpesaCallbackRequest callback) {
        log.info("Processing M-Pesa callback: {}", callback);

        Policy policy = policyRepository.findById(callback.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + callback.getPolicyId()));

        if (callback.getResultCode() != 0) {
            log.error("M-Pesa payment failed for policy {}: {}", callback.getPolicyId(), callback.getResultDesc());
            policy.setStatus(PolicyStatus.FAILED);
            policyRepository.save(policy);
            String messageForAgent = String.format("Payment failed for policy %d. Reason: %s",
                    policy.getId(), callback.getResultDesc());
            notificationService.createNotification(null, policy.getAgent().getUser(), messageForAgent);
            return;
        }

        // --- 1. Deduct Certificate Stock ---
        Product product = policy.getProduct();
        Superagent superagent = policy.getAgent().getSuperagent();
        CertificateStock stock = certificateStockRepository
                .findBySuperagentAndInsuranceCompanyAndProductClass(superagent, product.getInsuranceCompany(), product.getName())
                .orElseThrow(() -> new IllegalStateException(
                        String.format("No certificate stock for product '%s' from company '%s'",
                                product.getName(), product.getInsuranceCompany().getName())));
        if (stock.getQuantity() < 1) {
            throw new IllegalStateException("Insufficient certificate stock for this product.");
        }
        stock.setQuantity(stock.getQuantity() - 1);
        certificateStockRepository.save(stock);
        log.info("Deducted one certificate from stock. New balance for '{}': {}", product.getName(), stock.getQuantity());

        // --- 2. Update Policy Status and Dates ---
        policy.setStatus(PolicyStatus.PAID);
        policy.setPaidAt(LocalDateTime.now());
        policy.setStartDate(policy.getPaidAt());
        policy.setExpiryDate(policy.getPaidAt().plusYears(1));
        policyRepository.save(policy);

        // --- 3. Handle Commission ---
        Agent agent = policy.getAgent();
        BigDecimal commissionAmount = policy.getPremiumAmount().multiply(BigDecimal.valueOf(0.10));
        Wallet agentWallet = walletRepository.findByUser(agent.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for agent: " + agent.getUser().getFullName()));
        agentWallet.setBalance(agentWallet.getBalance().add(commissionAmount));
        walletRepository.save(agentWallet);

        // --- 4. Log Transaction ---
        Transaction commissionTransaction = Transaction.builder()
                .wallet(agentWallet)
                .policy(policy)
                .amount(commissionAmount)
                .transactionType(TransactionType.COMMISSION_EARNED)
                .status(TransactionStatus.COMPLETED)
                .build();
        transactionRepository.save(commissionTransaction);

        // --- 5. Generate Certificate ---
        String certificateUrl = pdfGeneratorUtil.generatePolicyCertificate(policy);
        Certificate certificate = Certificate.builder()
                .policy(policy)
                .fileUrl(certificateUrl)
                .build();
        certificateRepository.save(certificate);

        policy.setCertificateUrl(certificateUrl);
        policyRepository.save(policy);

        // --- 6. Send Notifications ---
        String messageForAgent = String.format("Payment of KES %.2f received for policy %d. Your commission of KES %.2f has been credited.",
                policy.getTotalAmount(), policy.getId(), commissionAmount);
        notificationService.createNotification(null, agent.getUser(), messageForAgent);

        String messageForSuperagent = String.format("Agent %s has sold a policy (%d). Payment of KES %.2f received.",
                agent.getUser().getFullName(), policy.getId(), policy.getTotalAmount());
        notificationService.createNotification(agent.getUser(), superagent.getUser(), messageForSuperagent);

        log.info("Successfully processed payment for policy {}. Agent {} earned commission of {}",
                policy.getId(), agent.getUser().getFullName(), commissionAmount);
    }
}