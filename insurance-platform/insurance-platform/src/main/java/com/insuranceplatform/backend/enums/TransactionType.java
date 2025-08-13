package com.insuranceplatform.backend.enums;

public enum TransactionType {
    COMMISSION_EARNED,
    WITHDRAWAL_REQUEST,
    WITHDRAWAL_COMPLETED,
    WITHDRAWAL_REJECTED,
    WITHDRAWAL_PROCESSING, 
    PAYOUT_DEBIT // A transaction for the Superagent when they pay an agent
}