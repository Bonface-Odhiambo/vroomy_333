package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Transaction;
import com.insuranceplatform.backend.entity.Wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.insuranceplatform.backend.entity.Superagent;

import java.util.stream.Stream;
import org.springframework.stereotype.Repository;
import com.insuranceplatform.backend.enums.TransactionStatus;
import com.insuranceplatform.backend.enums.TransactionType; 
import java.math.BigDecimal; 
import java.util.Optional;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE t.wallet.user.role = 'AGENT' AND t.status = :status")
    List<Transaction> findAgentTransactionsByStatus(TransactionStatus status);
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionType = :type")
    BigDecimal sumAmountByTransactionType(TransactionType type);
    @Query("SELECT t FROM Transaction t")
    Stream<Transaction> streamAll();
     @Query("SELECT t FROM Transaction t WHERE t.wallet.user.agentProfile.superagent = :superagent AND t.status = com.insuranceplatform.backend.enums.TransactionStatus.PENDING")
    List<Transaction> findPendingWithdrawalsForSuperagent(@Param("superagent") Superagent superagent);
    List<Transaction> findByWalletOrderByTimestampDesc(Wallet wallet);
    // In TransactionRepository.java
    Optional<Transaction> findByOriginatorConversationId(String originatorConversationId);
}