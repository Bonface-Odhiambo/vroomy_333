package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.MpesaCallbackRequest;
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

        policy.setStatus(PolicyStatus.PAID);
        policy.setPaidAt(LocalDateTime.now());
        policy.setStartDate(policy.getPaidAt());
        policy.setExpiryDate(policy.getPaidAt().plusYears(1));
        policyRepository.save(policy);

        Agent agent = policy.getAgent();
        BigDecimal commissionAmount = policy.getPremiumAmount().multiply(BigDecimal.valueOf(0.10));

        Wallet agentWallet = walletRepository.findByUser(agent.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for agent: " + agent.getUser().getFullName()));

        agentWallet.setBalance(agentWallet.getBalance().add(commissionAmount));
        walletRepository.save(agentWallet);

        // --- THIS IS THE CORRECTED PART ---
        Transaction commissionTransaction = Transaction.builder()
                .wallet(agentWallet)
                .policy(policy)
                .amount(commissionAmount)
                .transactionType(TransactionType.COMMISSION_EARNED) // Use the Enum constant
                .status(TransactionStatus.COMPLETED) // Commission is always completed instantly
                .build();
        // --- END OF CORRECTION ---
        transactionRepository.save(commissionTransaction);

        String certificateUrl = pdfGeneratorUtil.generatePolicyCertificate(policy);
        Certificate certificate = Certificate.builder()
                .policy(policy)
                .fileUrl(certificateUrl)
                .build();
        certificateRepository.save(certificate);

        policy.setCertificateUrl(certificateUrl);
        policyRepository.save(policy);

        Superagent superagent = agent.getSuperagent();
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