package com.bloxbean.cardano.yaci.store.mcp.server.service;

import com.bloxbean.cardano.yaci.store.api.transaction.service.TransactionService;
import com.bloxbean.cardano.yaci.store.mcp.server.model.PagedResult;
import com.bloxbean.cardano.yaci.store.mcp.server.model.PaginationCursor;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TransactionNetTransfer;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TransactionNetTransferDto;
import com.bloxbean.cardano.yaci.store.mcp.server.repository.TransactionNetTransferRepository;
import com.bloxbean.cardano.yaci.store.mcp.server.util.TransactionNetTransferCalculator;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for batch net transfer analysis.
 * Groups DTO results by transaction and optionally enriches with token data.
 */
@Service
@RequiredArgsConstructor
public class TransactionNetTransferBatchService {

    private final TransactionNetTransferRepository repository;
    private final TransactionService transactionService;

    /**
     * Get net transfers for transactions in slot range.
     *
     * @param startSlot Start slot (inclusive)
     * @param endSlot End slot (inclusive)
     * @param limit Maximum number of transactions
     * @param minNetLovelace Minimum absolute net lovelace to include
     * @param sortBy Sort order
     * @param includeTokens Whether to include token movements (requires additional processing)
     * @return List of transaction net transfers
     */
    public List<TransactionNetTransfer> getNetTransfersBatch(
            Long startSlot,
            Long endSlot,
            Integer limit,
            Long minNetLovelace,
            String sortBy,
            boolean includeTokens) {

        // Get raw DTO results from database (ADA only, filtered & sorted)
        List<TransactionNetTransferDto> dtos = repository.getNetTransfersBatch(
                startSlot, endSlot, limit, minNetLovelace, sortBy);

        if (dtos.isEmpty()) {
            return Collections.emptyList();
        }

        // Group DTOs by transaction hash
        Map<String, List<TransactionNetTransferDto>> byTxHash = dtos.stream()
                .collect(Collectors.groupingBy(TransactionNetTransferDto::getTxHash));

        List<TransactionNetTransfer> results = new ArrayList<>();

        for (Map.Entry<String, List<TransactionNetTransferDto>> entry : byTxHash.entrySet()) {
            String txHash = entry.getKey();
            List<TransactionNetTransferDto> txDtos = entry.getValue();

            if (includeTokens) {
                // Full analysis including tokens - fetch transaction details
                TransactionNetTransfer fullTransfer = getFullNetTransfer(txHash);
                if (fullTransfer != null) {
                    results.add(fullTransfer);
                }
            } else {
                // ADA-only analysis from DTOs (fast path)
                TransactionNetTransfer adaOnlyTransfer = buildAdaOnlyTransfer(txDtos);
                results.add(adaOnlyTransfer);
            }
        }

        return results;
    }

    /**
     * Build TransactionNetTransfer from DTOs (ADA only, no tokens).
     * Fast path that doesn't require fetching full transaction details.
     */
    private TransactionNetTransfer buildAdaOnlyTransfer(List<TransactionNetTransferDto> dtos) {
        if (dtos.isEmpty()) {
            return null;
        }

        // All DTOs are for same transaction
        TransactionNetTransferDto first = dtos.get(0);

        Map<String, TransactionNetTransfer.NetTransferPerAddress> netTransfers = new HashMap<>();
        List<String> senders = new ArrayList<>();
        List<String> receivers = new ArrayList<>();

        for (TransactionNetTransferDto dto : dtos) {
            TransactionNetTransfer.NetTransferPerAddress perAddress =
                    TransactionNetTransfer.NetTransferPerAddress.builder()
                            .address(dto.getAddress())
                            .stakeAddress(dto.getStakeAddress())
                            .netLovelace(dto.getNetLovelace())
                            .netAssets(Collections.emptyMap())  // No token data in fast path
                            .isSender(dto.getIsSender())
                            .isReceiver(dto.getIsReceiver())
                            .totalInputs(dto.getInputLovelace())
                            .totalOutputs(dto.getOutputLovelace())
                            .build();

            netTransfers.put(dto.getAddress(), perAddress);

            if (dto.getIsSender()) {
                senders.add(dto.getAddress());
            }
            if (dto.getIsReceiver()) {
                receivers.add(dto.getAddress());
            }
        }

        // Calculate summary
        BigInteger totalAdaMoved = netTransfers.values().stream()
                .map(TransactionNetTransfer.NetTransferPerAddress::getNetLovelace)
                .map(BigInteger::abs)
                .reduce(BigInteger.ZERO, BigInteger::add)
                .divide(BigInteger.TWO);

        TransactionNetTransfer.TransferSummary summary =
                TransactionNetTransfer.TransferSummary.builder()
                        .totalAddresses(netTransfers.size())
                        .senderCount(senders.size())
                        .receiverCount(receivers.size())
                        .assetTypesCount(0)  // No tokens in fast path
                        .totalAdaMoved(totalAdaMoved)
                        .isSimpleTransfer(senders.size() == 1 && receivers.size() == 1)
                        .hasScriptInteraction(false)  // Unknown in fast path
                        .build();

        return TransactionNetTransfer.builder()
                .txHash(first.getTxHash())
                .blockHeight(first.getBlock())
                .slot(first.getSlot())
                .fee(first.getFee())
                .netTransfers(netTransfers)
                .senders(senders)
                .receivers(receivers)
                .summary(summary)
                .build();
    }

    /**
     * Get full net transfer including tokens by fetching transaction details.
     * Slower but complete analysis.
     */
    private TransactionNetTransfer getFullNetTransfer(String txHash) {
        Optional<TransactionDetails> txDetailsOpt = transactionService.getTransaction(txHash);
        if (txDetailsOpt.isEmpty()) {
            return null;
        }

        return TransactionNetTransferCalculator.calculate(txDetailsOpt.get());
    }

    /**
     * Get net transfers with cursor-based pagination.
     * Production-grade pagination for processing large datasets.
     *
     * @param startSlot Start slot (inclusive)
     * @param endSlot End slot (inclusive)
     * @param pageSize Results per page
     * @param cursor Pagination cursor (null for first page)
     * @param minNetLovelace Minimum absolute net lovelace to include
     * @param sortBy Sort order
     * @return Paged result with cursor for next page
     */
    public PagedResult<TransactionNetTransfer> getNetTransfersBatchPaged(
            Long startSlot,
            Long endSlot,
            Integer pageSize,
            PaginationCursor cursor,
            Long minNetLovelace,
            String sortBy) {

        // Get paged DTOs from repository
        PagedResult<TransactionNetTransferDto> dtoPage = repository.getNetTransfersBatchPaged(
                startSlot, endSlot, pageSize, cursor, minNetLovelace, sortBy);

        if (dtoPage.getResults().isEmpty()) {
            return PagedResult.<TransactionNetTransfer>builder()
                    .results(Collections.emptyList())
                    .pageSize(0)
                    .nextCursor(null)
                    .hasMore(false)
                    .totalProcessed(dtoPage.getTotalProcessed())
                    .build();
        }

        // Group DTOs by transaction hash
        Map<String, List<TransactionNetTransferDto>> byTxHash = dtoPage.getResults().stream()
                .collect(Collectors.groupingBy(TransactionNetTransferDto::getTxHash));

        List<TransactionNetTransfer> results = new ArrayList<>();

        // Build TransactionNetTransfer objects (ADA only for now)
        for (List<TransactionNetTransferDto> txDtos : byTxHash.values()) {
            TransactionNetTransfer transfer = buildAdaOnlyTransfer(txDtos);
            if (transfer != null) {
                results.add(transfer);
            }
        }

        return PagedResult.<TransactionNetTransfer>builder()
                .results(results)
                .pageSize(results.size())
                .nextCursor(dtoPage.getNextCursor())
                .hasMore(dtoPage.getHasMore())
                .totalProcessed(dtoPage.getTotalProcessed())
                .build();
    }
}
