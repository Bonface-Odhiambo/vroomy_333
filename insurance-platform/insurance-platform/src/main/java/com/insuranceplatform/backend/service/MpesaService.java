package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.dto.B2CResultCallback;
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

    // ===================================================================================
    // == C2B (CUSTOMER-TO-BUSINESS) FLOW: Receiving payments for policies
    // ===================================================================================

    /**
     * Initiates an STK Push to the client's phone for policy payment.
     */
    public String initiateStkPush(StkPushRequest request) {
        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + request.getPolicyId()));

        log.info("Initiating STK Push to phone {} for policy {} with amount {}",
                request.getPhoneNumber(), policy.getId(), policy.getTotalAmount());

        // --- DARAJA API INTEGRATION POINT ---
        // 1. Get Daraja Access Token.
        // 2. Build the STK Push request payload (BusinessShortCode, Password, Timestamp, Amount, PartyA, PartyB, etc.).
        // 3. Make an HTTP POST request to the Daraja STK Push endpoint.
        // 4. Handle the response. If successful, save the 'CheckoutRequestID' to the policy/transaction for reconciliation.
        // 5. If it fails, throw an exception.
        // --- END INTEGRATION POINT ---
        
        if ("0700000000".equals(request.getPhoneNumber())) {
            log.error("MOCK STK PUSH FAILED: Phone number {} is blacklisted for testing.", request.getPhoneNumber());
            throw new StkPushException("Failed to initiate STK Push. Please try again later.");
        }
        
        return "STK Push initiated successfully.";
    }

    /**
     * Provides Paybill details as a fallback if STK Push fails.
     */
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

    /**
     * Processes the callback from Safaricom after a C2B payment (STK Push).
     * This method contains all the core business logic for a successful sale.
     */
    @Transactional
    public void processMpesaCallback(MpesaCallbackRequest callback) {
        log.info("Processing M-Pesa C2B callback for policyId: {}", callback.getPolicyId());

        Policy policy = policyRepository.findById(callback.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + callback.getPolicyId()));

        if (callback.getResultCode() != 0) {
            handleFailedPayment(policy, callback.getResultDesc());
            return;
        }

        // 1. Update Policy
        updatePolicyOnPayment(policy);
        
        // 2. Handle Commission & Transaction Logging
        handleCommission(policy);
        
        // 3. Deduct Stock & Generate Certificate
        String certificateUrl = handleCertificate(policy);
        policy.setCertificateUrl(certificateUrl);
        policyRepository.save(policy);

        // 4. Send Notifications
        sendPaymentNotifications(policy);

        log.info("Successfully processed payment for policy {}.", policy.getId());
    }

    // ===================================================================================
    // == B2C (BUSINESS-TO-CUSTOMER) FLOW: Sending commission payouts to agents
    // ===================================================================================

    /**
     * Initiates a B2C M-Pesa payment from the superagent to an agent.
     */
    public void initiateB2CPayment(BigDecimal amount, String phoneNumber, String remarks, Long transactionId) {
        log.info("Initiating B2C payment of KES {} to {} for transactionId: {}. Remarks: {}", amount, phoneNumber, transactionId, remarks);

        // --- DARAJA API INTEGRATION POINT ---
        // 1. Get Daraja Access Token.
        // 2. Build the B2C request payload (InitiatorName, SecurityCredential, CommandID, Amount, PartyA, PartyB, Remarks, ResultURL etc.).
        // 3. Make an HTTP POST request to the Daraja B2C endpoint.
        // 4. On successful submission, Daraja returns an 'OriginatorConversationID'.
        // 5. Find the transaction in your DB by `transactionId` and save this 'OriginatorConversationID' to it for later lookup.
        //    Transaction tx = transactionRepository.findById(transactionId).get();
        //    tx.setOriginatorConversationId(darajaResponse.getOriginatorConversationID());
        //    transactionRepository.save(tx);
        // --- END INTEGRATION POINT ---
    }

    /**
     * Processes the callback from Safaricom after a B2C payout.
     */
    @Transactional
    public void processB2CResultCallback(B2CResultCallback callback) {
        String conversationId = callback.Result().originatorConversationID();
        log.info("Processing M-Pesa B2C callback for ConversationID: {}", conversationId);

        Transaction transaction = transactionRepository.findByOriginatorConversationId(conversationId)
                .orElse(null);

        if (transaction == null) {
            log.error("Received B2C callback for an unknown transaction. ConversationID: {}", conversationId);
            return;
        }

        if (callback.Result().resultCode() == 0) {
            handleSuccessfulPayout(transaction, callback);
        } else {
            handleFailedPayout(transaction, callback);
        }
    }

    // ===================================================================================
    // == Private Helper Methods
    // ===================================================================================

    private void handleFailedPayment(Policy policy, String reason) {
        log.error("M-Pesa payment failed for policy {}: {}", policy.getId(), reason);
        policy.setStatus(PolicyStatus.FAILED);
        policyRepository.save(policy);
        String message = String.format("Payment failed for policy %d. Reason: %s", policy.getId(), reason);
        notificationService.createNotification(null, policy.getAgent().getUser(), message);
    }

    private void updatePolicyOnPayment(Policy policy) {
        policy.setStatus(PolicyStatus.PAID);
        policy.setPaidAt(LocalDateTime.now());
        policy.setStartDate(policy.getPaidAt());
        policy.setExpiryDate(policy.getPaidAt().plusYears(1));
        policyRepository.save(policy);
    }

    private void handleCommission(Policy policy) {
        Agent agent = policy.getAgent();
        BigDecimal commissionAmount = policy.getPremiumAmount().multiply(BigDecimal.valueOf(0.10)); // 10% commission
        Wallet agentWallet = walletRepository.findByUser(agent.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for agent: " + agent.getUser().getFullName()));
        
        agentWallet.setBalance(agentWallet.getBalance().add(commissionAmount));
        walletRepository.save(agentWallet);

        transactionRepository.save(Transaction.builder()
                .wallet(agentWallet)
                .policy(policy)
                .amount(commissionAmount)
                .transactionType(TransactionType.COMMISSION_EARNED)
                .status(TransactionStatus.COMPLETED)
                .build());
    }

    private String handleCertificate(Policy policy) {
        Product product = policy.getProduct();
        Superagent superagent = policy.getAgent().getSuperagent();
        CertificateStock stock = certificateStockRepository
                .findBySuperagentAndInsuranceCompanyAndProductClass(superagent, product.getInsuranceCompany(), product.getName())
                .orElseThrow(() -> new IllegalStateException(String.format("No certificate stock for product '%s'", product.getName())));
        
        if (stock.getQuantity() < 1) {
            throw new IllegalStateException("Insufficient certificate stock for this product.");
        }
        stock.setQuantity(stock.getQuantity() - 1);
        certificateStockRepository.save(stock);

        String certificateUrl = pdfGeneratorUtil.generatePolicyCertificate(policy);
        certificateRepository.save(Certificate.builder().policy(policy).fileUrl(certificateUrl).build());
        return certificateUrl;
    }

    private void sendPaymentNotifications(Policy policy) {
        Agent agent = policy.getAgent();
        BigDecimal commissionAmount = policy.getPremiumAmount().multiply(BigDecimal.valueOf(0.10));
        String agentMsg = String.format("Payment of KES %.2f received for policy %d. Your commission of KES %.2f has been credited.",
                policy.getTotalAmount(), policy.getId(), commissionAmount);
        notificationService.createNotification(null, agent.getUser(), agentMsg);

        String superagentMsg = String.format("Agent %s sold a policy (%d). Payment of KES %.2f received.",
                agent.getUser().getFullName(), policy.getId(), policy.getTotalAmount());
        notificationService.createNotification(agent.getUser(), agent.getSuperagent().getUser(), superagentMsg);
    }
    
    private void handleSuccessfulPayout(Transaction transaction, B2CResultCallback callback) {
        log.info("B2C Payment Successful for transactionId: {}. M-Pesa Transaction ID: {}", transaction.getId(), callback.Result().transactionID());
        
        // Update transaction status
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTransactionType(TransactionType.WITHDRAWAL_COMPLETED);
        // transaction.setMpesaTransactionId(callback.Result().transactionID()); // Add this field to your Transaction entity
        
        // Find Superagent and debit their wallet (as an internal record)
        Agent agent = transaction.getWallet().getUser().getAgentProfile();
        Superagent superagent = agent.getSuperagent();
        Wallet superagentWallet = walletRepository.findByUser(superagent.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for superagent: " + superagent.getUser().getFullName()));
        
        transactionRepository.save(Transaction.builder()
                .wallet(superagentWallet)
                .amount(transaction.getAmount().negate()) // Debit is a negative amount
                .transactionType(TransactionType.PAYOUT_DEBIT)
                .status(TransactionStatus.COMPLETED)
                .build());
        
        // Send success notification to Agent
        String message = String.format("Your withdrawal of KES %.2f has been successfully sent to your M-Pesa.", transaction.getAmount());
        notificationService.createNotification(superagent.getUser(), agent.getUser(), message);
        
        transactionRepository.save(transaction);
    }

    private void handleFailedPayout(Transaction transaction, B2CResultCallback callback) {
        log.error("B2C Payment Failed for transactionId: {}. Reason: {}", transaction.getId(), callback.Result().resultDesc());

        // Mark the transaction as rejected
        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setTransactionType(TransactionType.WITHDRAWAL_REJECTED); // Add this enum value

        // CRITICAL: Refund the money back to the agent's internal wallet
        Wallet agentWallet = transaction.getWallet();
        agentWallet.setBalance(agentWallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(agentWallet);

        // Send failure notification to Agent
        String message = String.format("Your withdrawal of KES %.2f failed. Reason: %s. The amount has been refunded to your wallet.",
                transaction.getAmount(), callback.Result().resultDesc());
        Agent agent = agentWallet.getUser().getAgentProfile();
        notificationService.createNotification(agent.getSuperagent().getUser(), agent.getUser(), message);

        transactionRepository.save(transaction);
    }
}