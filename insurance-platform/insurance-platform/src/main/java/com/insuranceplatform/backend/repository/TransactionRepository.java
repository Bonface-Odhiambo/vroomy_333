package com.insuranceplatform.backend.repository;

import com.insuranceplatform.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.insuranceplatform.backend.enums.TransactionStatus;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE t.wallet.user.role = 'AGENT' AND t.status = :status")
    List<Transaction> findAgentTransactionsByStatus(TransactionStatus status);
}