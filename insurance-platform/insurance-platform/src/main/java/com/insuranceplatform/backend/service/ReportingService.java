package com.insuranceplatform.backend.service;

import com.insuranceplatform.backend.entity.Transaction;
import com.insuranceplatform.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final TransactionRepository transactionRepository;

    public String generateAgentTransactionsCsv() {
        List<Transaction> transactions = transactionRepository.findAll();
        StringBuilder csvBuilder = new StringBuilder();

        // Add header row
        csvBuilder.append("TransactionID,PolicyID,WalletID,UserID,Amount,TransactionType,Timestamp\n");

        // Add data rows
        for (Transaction tx : transactions) {
            csvBuilder.append(tx.getId()).append(",");
            csvBuilder.append(tx.getPolicy() != null ? tx.getPolicy().getId() : "N/A").append(",");
            csvBuilder.append(tx.getWallet().getId()).append(",");
            csvBuilder.append(tx.getWallet().getUser().getId()).append(",");
            csvBuilder.append(tx.getAmount()).append(",");
            csvBuilder.append(tx.getTransactionType()).append(",");
            csvBuilder.append(tx.getTimestamp()).append("\n");
        }

        return csvBuilder.toString();
    }
}