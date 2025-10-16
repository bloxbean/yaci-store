package com.bloxbean.cardano.yaci.store.mcp.server.service;

import com.bloxbean.cardano.yaci.store.mcp.server.model.AddressActivityPage;
import com.bloxbean.cardano.yaci.store.mcp.server.model.AddressNetActivitySummary;
import com.bloxbean.cardano.yaci.store.mcp.server.repository.TransactionNetTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for address-level net activity aggregation.
 *
 * Aggregates transaction net transfers by address to provide a high-level view
 * of address activity across multiple transactions.
 */
@Service
@RequiredArgsConstructor
public class AddressNetActivityService {

    private final TransactionNetTransferRepository repository;

    /**
     * Get aggregated net activity per address for a time range.
     *
     * This is much more efficient than transaction-level analysis for large datasets
     * because it returns O(unique addresses) instead of O(transactions × addresses).
     *
     * @param startSlot Start slot (inclusive)
     * @param endSlot End slot (inclusive)
     * @param maxTransactions Maximum number of transactions to analyze
     * @param minTotalNetLovelace Minimum absolute net lovelace per address to include
     * @param sortBy Sort order
     * @param page Page number (0-based)
     * @param pageSize Results per page
     * @return Page of address activity summaries
     */
    public AddressActivityPage getAddressNetActivity(
            Long startSlot,
            Long endSlot,
            Integer maxTransactions,
            Long minTotalNetLovelace,
            String sortBy,
            Integer page,
            Integer pageSize) {

        List<AddressNetActivitySummary> addresses = repository.getAddressNetActivityAggregated(
                startSlot, endSlot, maxTransactions, minTotalNetLovelace, sortBy, page, pageSize
        );

        return AddressActivityPage.builder()
                .addresses(addresses)
                .totalAddresses(addresses.size())
                .page(page)
                .pageSize(pageSize)
                .totalTransactionsAnalyzed((long) maxTransactions)
                .startSlot(startSlot)
                .endSlot(endSlot)
                .build();
    }
}
