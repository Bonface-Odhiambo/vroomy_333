package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.insuranceplatform.backend.enums.TransactionStatus;
import com.insuranceplatform.backend.enums.TransactionType; 
import org.springframework.data.jpa.repository.Query; 
import java.math.BigDecimal; 

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE t.wallet.user.role = 'AGENT' AND t.status = :status")
    List<Transaction> findAgentTransactionsByStatus(TransactionStatus status);
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionType = :type")
    BigDecimal sumAmountByTransactionType(TransactionType type);
}