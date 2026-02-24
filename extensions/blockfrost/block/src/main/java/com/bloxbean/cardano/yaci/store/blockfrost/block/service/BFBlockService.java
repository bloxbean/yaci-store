package com.bloxbean.cardano.yaci.store.blockfrost.block.service;

import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockAddressTxDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockTxCborDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.mapper.BFBlockMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.BFBlocksStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockAddressTxRow;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockRow;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BFBlockService {
    private static final String NOT_FOUND_MESSAGE = "The requested component has not been found.";

    private final BFBlocksStorageReader bfBlocksStorageReader;
    private final ObjectProvider<TransactionStoreProperties> transactionStorePropertiesProvider;
    private final BFBlockMapper bfBlockMapper = BFBlockMapper.INSTANCE;

    public BFBlockDTO getLatestBlock() {
        return bfBlockMapper.toBFBlockDTO(requireLatestBlock());
    }

    public BFBlockDTO getBlock(String hashOrNumber) {
        return bfBlockMapper.toBFBlockDTO(requireBlock(hashOrNumber));
    }

    public BFBlockDTO getBlockBySlot(long slotNumber) {
        BFBlockRow blockRow = bfBlocksStorageReader.findBlockBySlot(slotNumber)
                .orElseThrow(this::notFound);

        return bfBlockMapper.toBFBlockDTO(blockRow);
    }

    public BFBlockDTO getBlockByEpochAndSlot(int epochNumber, int epochSlot) {
        BFBlockRow blockRow = bfBlocksStorageReader.findBlockByEpochAndEpochSlot(epochNumber, epochSlot)
                .orElseThrow(this::notFound);

        return bfBlockMapper.toBFBlockDTO(blockRow);
    }

    public List<BFBlockDTO> getNextBlocks(String hashOrNumber, int page, int count) {
        BFBlockRow currentBlock = requireBlock(hashOrNumber);
        long blockNumber = requireBlockNumber(currentBlock);

        return bfBlocksStorageReader.findNextBlocks(blockNumber, page, count)
                .stream()
                .map(bfBlockMapper::toBFBlockDTO)
                .toList();
    }

    public List<BFBlockDTO> getPreviousBlocks(String hashOrNumber, int page, int count) {
        BFBlockRow currentBlock = requireBlock(hashOrNumber);
        long blockNumber = requireBlockNumber(currentBlock);

        return bfBlocksStorageReader.findPreviousBlocks(blockNumber, page, count)
                .stream()
                .map(bfBlockMapper::toBFBlockDTO)
                .toList();
    }

    public List<String> getLatestBlockTxHashes(int page, int count, Order order) {
        BFBlockRow latestBlock = requireLatestBlock();
        long blockNumber = requireBlockNumber(latestBlock);

        return bfBlocksStorageReader.findBlockTxHashes(blockNumber, page, count, order);
    }

    public List<String> getBlockTxHashes(String hashOrNumber, int page, int count, Order order) {
        BFBlockRow block = requireBlock(hashOrNumber);
        long blockNumber = requireBlockNumber(block);

        return bfBlocksStorageReader.findBlockTxHashes(blockNumber, page, count, order);
    }

    public List<BFBlockTxCborDTO> getLatestBlockTxsCbor(int page, int count, Order order) {
        ensureTransactionCborEnabled();

        BFBlockRow latestBlock = requireLatestBlock();
        long blockNumber = requireBlockNumber(latestBlock);

        return bfBlocksStorageReader.findBlockTxCbor(blockNumber, page, count, order)
                .stream()
                .map(bfBlockMapper::toBFBlockTxCborDTO)
                .toList();
    }

    public List<BFBlockTxCborDTO> getBlockTxsCbor(String hashOrNumber, int page, int count, Order order) {
        ensureTransactionCborEnabled();

        BFBlockRow block = requireBlock(hashOrNumber);
        long blockNumber = requireBlockNumber(block);

        return bfBlocksStorageReader.findBlockTxCbor(blockNumber, page, count, order)
                .stream()
                .map(bfBlockMapper::toBFBlockTxCborDTO)
                .toList();
    }

    public List<BFBlockAddressDTO> getBlockAddresses(String hashOrNumber, int page, int count) {
        BFBlockRow block = requireBlock(hashOrNumber);
        long blockNumber = requireBlockNumber(block);

        List<BFBlockAddressTxRow> rows = bfBlocksStorageReader.findBlockAddressTransactions(blockNumber, page, count);

        Map<String, LinkedHashMap<String, BFBlockAddressTxDTO>> addresses = new LinkedHashMap<>();
        for (BFBlockAddressTxRow row : rows) {
            if (row.address() == null || row.txHash() == null) {
                continue;
            }

            var txsByHash = addresses.computeIfAbsent(row.address(), key -> new LinkedHashMap<>());
            txsByHash.putIfAbsent(row.txHash(), bfBlockMapper.toBFBlockAddressTxDTO(row));
        }

        return addresses.entrySet()
                .stream()
                .map(entry -> BFBlockAddressDTO.builder()
                        .address(entry.getKey())
                        .transactions(entry.getValue().values().stream().toList())
                        .build())
                .toList();
    }

    private BFBlockRow requireLatestBlock() {
        return bfBlocksStorageReader.findLatestBlock()
                .orElseThrow(this::notFound);
    }

    private BFBlockRow requireBlock(String hashOrNumber) {
        Optional<Long> blockNumber = parseBlockNumber(hashOrNumber);

        return blockNumber
                .flatMap(bfBlocksStorageReader::findBlockByNumber)
                .or(() -> bfBlocksStorageReader.findBlockByHash(hashOrNumber))
                .orElseThrow(this::notFound);
    }

    private long requireBlockNumber(BFBlockRow blockRow) {
        if (blockRow == null || blockRow.height() == null) {
            throw notFound();
        }

        return blockRow.height();
    }

    private void ensureTransactionCborEnabled() {
        TransactionStoreProperties transactionStoreProperties = transactionStorePropertiesProvider.getIfAvailable();
        if (transactionStoreProperties == null || !transactionStoreProperties.isSaveCbor()) {
            throw notFound();
        }
    }

    private Optional<Long> parseBlockNumber(String hashOrNumber) {
        if (hashOrNumber == null || hashOrNumber.isBlank()) {
            return Optional.empty();
        }

        // Hash is expected as a 64-char hex string, so treat 64-char inputs as hash first.
        if (hashOrNumber.length() == 64) {
            return Optional.empty();
        }

        if (!hashOrNumber.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(hashOrNumber));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE);
    }
}
