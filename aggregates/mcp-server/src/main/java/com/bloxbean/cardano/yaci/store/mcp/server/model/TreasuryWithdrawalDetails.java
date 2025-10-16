package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Model representing enacted treasury withdrawals and proposal refunds.
 * Tracks actual ADA movements from the treasury to recipients.
 */
public record TreasuryWithdrawalDetails(
    String address,                 // Recipient address
    String type,                    // Type: 'treasury' for withdrawals, 'proposal_refund' for refunds
    BigDecimal amount,              // Amount in lovelace
    Integer earnedEpoch,            // Epoch when funds were earned/allocated
    Integer spendableEpoch,         // Epoch when funds become spendable
    Long slot                       // Slot when recorded
) {}
