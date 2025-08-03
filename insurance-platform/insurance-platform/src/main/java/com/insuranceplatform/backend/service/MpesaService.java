package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.MpesaCallbackRequest;
import com.insuranceplatform.backend.dto.StkPushRequest;
import com.insuranceplatform.backend.entity.*;
import com.insuranceplatform.backend.enums.PolicyStatus;
import com.insuranceplatform.backend.exception.ResourceNotFoundException;
import com.insuranceplatform.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j // For logging
public class MpesaService {

    private final PolicyRepository policyRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CertificateRepository certificateRepository;
    private final NotificationService notificationService; // Injected for notifications

    public String initiateStkPush(StkPushRequest request) {
        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + request.getPolicyId()));

        // TODO: In a real application, this is where you would call the Safaricom Daraja API.
        // For now, we just log and simulate success.
        log.info("Simulating STK Push to phone {} for policy {} with amount {}",
                request.getPhoneNumber(), policy.getId(), policy.getTotalAmount());

        return "STK Push initiated successfully (simulation).";
    }

    @Transactional
    public void processMpesaCallback(MpesaCallbackRequest callback) {
        log.info("Processing M-Pesa callback: {}", callback);

        if (callback.getResultCode() != 0) {
            log.error("M-Pesa payment failed for policy {}: {}", callback.getPolicyId(), callback.getResultDesc());
            // TODO: Update policy status to FAILED or notify agent
            return;
        }

        Policy policy = policyRepository.findById(callback.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + callback.getPolicyId()));

        // --- 1. Update Policy ---
        policy.setStatus(PolicyStatus.PAID);
        policy.setPaidAt(LocalDateTime.now());
        policyRepository.save(policy);

        // --- 2. Handle Commission ---
        Agent agent = policy.getAgent();
        BigDecimal commissionAmount = policy.getPremiumAmount().multiply(BigDecimal.valueOf(0.10)); // 10% commission

        Wallet agentWallet = walletRepository.findByUser(agent.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for agent: " + agent.getUser().getFullName()));

        agentWallet.setBalance(agentWallet.getBalance().add(commissionAmount));
        walletRepository.save(agentWallet);

        // --- 3. Log Transactions ---
        // Log commission earned by the agent
        Transaction commissionTransaction = Transaction.builder()
                .wallet(agentWallet)
                .policy(policy)
                .amount(commissionAmount)
                .transactionType("COMMISSION_EARNED")
                .build();
        transactionRepository.save(commissionTransaction);

        // --- 4. Generate Certificate ---
        // TODO: This would involve a real PDF generation library
        String certificateUrl = "/certificates/cert_" + policy.getId() + ".pdf";
        Certificate certificate = Certificate.builder()
                .policy(policy)
                .fileUrl(certificateUrl)
                .build();
        certificateRepository.save(certificate);

        // Update the policy with the certificate URL
        policy.setCertificateUrl(certificateUrl);
        policyRepository.save(policy);

        // --- 5. Send Notifications (NEW) ---
        Superagent superagent = agent.getSuperagent();
        String messageForAgent = String.format("Payment of KES %.2f received for policy %d. Your commission of KES %.2f has been credited.",
                policy.getTotalAmount(), policy.getId(), commissionAmount);
        notificationService.createNotification(null, agent.getUser(), messageForAgent); // Sender is system (null)

        String messageForSuperagent = String.format("Agent %s has sold a policy (%d). Payment of KES %.2f received.",
                agent.getUser().getFullName(), policy.getId(), policy.getTotalAmount());
        notificationService.createNotification(agent.getUser(), superagent.getUser(), messageForSuperagent);

        log.info("Successfully processed payment for policy {}. Agent {} earned commission of {}",
                policy.getId(), agent.getUser().getFullName(), commissionAmount);
    }
}