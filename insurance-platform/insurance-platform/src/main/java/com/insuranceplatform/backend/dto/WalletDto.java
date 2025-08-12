package com.insuranceplatform.backend.dto;

import com.insuranceplatform.backend.entity.Transaction;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for representing an Agent's wallet details, including
 * the current balance and a history of transactions.
 */
public record WalletDto(
    BigDecimal balance,
    List<Transaction> transactionHistory
) {}