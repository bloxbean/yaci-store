package com.bloxbean.cardano.yaci.store.blockfrost.block.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.dto.BFBlockTxCborDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.block.service.BFBlockService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Blocks")
@RequestMapping("${blockfrost.apiPrefix}/blocks")
@ConditionalOnExpression("${store.extensions.blockfrost.blocks.enabled:false}")
public class BFBlockController {

    private final BFBlockService bfBlockService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost BlocksController initialized >>>");
    }

    @GetMapping("latest")
    @Operation(summary = "Latest block", description = "Return the latest block available in the store.")
    public BFBlockDTO getLatestBlock() {
        return bfBlockService.getLatestBlock();
    }

    @GetMapping("latest/txs")
    @Operation(summary = "Latest block transactions", description = "Return transaction hashes in the latest block.")
    public List<String> getLatestBlockTxs(
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order
    ) {
        int p = page - 1;
        return bfBlockService.getLatestBlockTxHashes(p, count, order);
    }

    @GetMapping("latest/txs/cbor")
    @Operation(summary = "Latest block transactions with CBOR", description = "Return transaction hashes and CBOR in the latest block.")
    public List<BFBlockTxCborDTO> getLatestBlockTxsCbor(
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order
    ) {
        int p = page - 1;
        return bfBlockService.getLatestBlockTxsCbor(p, count, order);
    }

    @GetMapping("slot/{slot_number}")
    @Operation(summary = "Block by slot", description = "Return block content for the provided absolute slot number.")
    public BFBlockDTO getBlockBySlot(@PathVariable("slot_number") long slotNumber) {
        return bfBlockService.getBlockBySlot(slotNumber);
    }

    @GetMapping("epoch/{epoch_number}/slot/{slot_number}")
    @Operation(summary = "Block by epoch and slot", description = "Return block content for absolute slot constrained by epoch.")
    public BFBlockDTO getBlockByEpochAndSlot(@PathVariable("epoch_number") int epochNumber,
                                             @PathVariable("slot_number") long slotNumber) {
        return bfBlockService.getBlockByEpochAndSlot(epochNumber, slotNumber);
    }

    @GetMapping("{hash_or_number}")
    @Operation(summary = "Specific block", description = "Return block content by hash or block number.")
    public BFBlockDTO getBlock(@PathVariable("hash_or_number") String hashOrNumber) {
        return bfBlockService.getBlock(hashOrNumber);
    }

    @GetMapping("{hash_or_number}/next")
    @Operation(summary = "Next blocks", description = "Return blocks following the specified block.")
    public List<BFBlockDTO> getNextBlocks(
            @PathVariable("hash_or_number") String hashOrNumber,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        int p = page - 1;
        return bfBlockService.getNextBlocks(hashOrNumber, p, count);
    }

    @GetMapping("{hash_or_number}/previous")
    @Operation(summary = "Previous blocks", description = "Return blocks preceding the specified block.")
    public List<BFBlockDTO> getPreviousBlocks(
            @PathVariable("hash_or_number") String hashOrNumber,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        int p = page - 1;
        return bfBlockService.getPreviousBlocks(hashOrNumber, p, count);
    }

    @GetMapping("{hash_or_number}/txs")
    @Operation(summary = "Block transactions", description = "Return transaction hashes in the specified block.")
    public List<String> getBlockTxs(
            @PathVariable("hash_or_number") String hashOrNumber,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order
    ) {
        int p = page - 1;
        return bfBlockService.getBlockTxHashes(hashOrNumber, p, count, order);
    }

    @GetMapping("{hash_or_number}/txs/cbor")
    @Operation(summary = "Block transactions with CBOR", description = "Return transaction hashes and CBOR in the specified block.")
    public List<BFBlockTxCborDTO> getBlockTxsCbor(
            @PathVariable("hash_or_number") String hashOrNumber,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order
    ) {
        int p = page - 1;
        return bfBlockService.getBlockTxsCbor(hashOrNumber, p, count, order);
    }

    @GetMapping("{hash_or_number}/addresses")
    @Operation(summary = "Affected addresses", description = "Return affected addresses for the specified block.")
    public List<BFBlockAddressDTO> getBlockAddresses(
            @PathVariable("hash_or_number") String hashOrNumber,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        int p = page - 1;
        return bfBlockService.getBlockAddresses(hashOrNumber, p, count);
    }
}
