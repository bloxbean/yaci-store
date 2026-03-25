package com.bloxbean.cardano.yaci.store.blockfrost.transaction.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.service.BFTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Transactions")
@RequestMapping("${blockfrost.apiPrefix}/txs")
@ConditionalOnExpression("${store.extensions.blockfrost.transaction.enabled:false}")
public class BFTransactionController {

    private final BFTransactionService bfTransactionService;

    @GetMapping("/{hash}")
    @Operation(summary = "Specific transaction", description = "Return content of a specific transaction.")
    public BFTransactionDto getTransaction(@PathVariable String hash) {
        return bfTransactionService.getTransaction(hash);
    }

    @GetMapping("/{hash}/utxos")
    @Operation(summary = "Transaction UTXOs", description = "Return inputs and outputs of a specific transaction.")
    public BFTxUtxosDto getTxUtxos(@PathVariable String hash) {
        return bfTransactionService.getTxUtxos(hash);
    }

    @GetMapping("/{hash}/cbor")
    @Operation(summary = "Transaction CBOR", description = "Return CBOR serialized transaction.")
    public BFTxCborDto getTxCbor(@PathVariable String hash) {
        return bfTransactionService.getTxCbor(hash);
    }

    @GetMapping("/{hash}/metadata")
    @Operation(summary = "Transaction metadata", description = "Obtain the transaction metadata.")
    public List<BFTxMetadataDto> getTxMetadata(@PathVariable String hash) {
        return bfTransactionService.getTxMetadata(hash);
    }

    @GetMapping("/{hash}/metadata/cbor")
    @Operation(summary = "Transaction metadata in CBOR", description = "Obtain the transaction metadata in CBOR.")
    public List<Object> getTxMetadataCbor(@PathVariable String hash) {
        // TODO: CBOR-encoded metadata is not currently stored. Implement when CBOR metadata storage is available.
        return Collections.emptyList();
    }

    @GetMapping("/{hash}/redeemers")
    @Operation(summary = "Transaction redeemers", description = "Obtain the transaction redeemers.")
    public List<BFTxRedeemerDto> getTxRedeemers(@PathVariable String hash) {
        return bfTransactionService.getTxRedeemers(hash);
    }

    @GetMapping("/{hash}/stakes")
    @Operation(summary = "Transaction stake addresses certificates", description = "Obtain information about (de)registration of stake addresses within a specific transaction.")
    public List<BFTxStakeDto> getTxStakes(@PathVariable String hash) {
        return bfTransactionService.getTxStakes(hash);
    }

    @GetMapping("/{hash}/delegations")
    @Operation(summary = "Transaction delegation certificates", description = "Obtain information about delegation certificates of a specific transaction.")
    public List<BFTxDelegationDto> getTxDelegations(@PathVariable String hash) {
        return bfTransactionService.getTxDelegations(hash);
    }

    @GetMapping("/{hash}/withdrawals")
    @Operation(summary = "Transaction withdrawals", description = "Obtain information about withdrawals of a specific transaction.")
    public List<BFTxWithdrawalDto> getTxWithdrawals(@PathVariable String hash) {
        return bfTransactionService.getTxWithdrawals(hash);
    }

    @GetMapping("/{hash}/mirs")
    @Operation(summary = "Transaction MIRs", description = "Obtain information about Move Instantaneous Rewards (MIR) of a specific transaction.")
    public List<BFTxMirDto> getTxMirs(@PathVariable String hash) {
        return bfTransactionService.getTxMirs(hash);
    }

    @GetMapping("/{hash}/pool_updates")
    @Operation(summary = "Transaction pool registration and update certificates", description = "Obtain information about stake pool registration and update certificates of a specific transaction.")
    public List<BFTxPoolUpdateDto> getTxPoolUpdates(@PathVariable String hash) {
        return bfTransactionService.getTxPoolUpdates(hash);
    }

    @GetMapping("/{hash}/pool_retires")
    @Operation(summary = "Transaction pool retirement certificates", description = "Obtain information about stake pool retirements within a specific transaction.")
    public List<BFTxPoolRetireDto> getTxPoolRetires(@PathVariable String hash) {
        return bfTransactionService.getTxPoolRetires(hash);
    }

    @GetMapping("/{hash}/required_signers")
    @Operation(summary = "Transaction required signers", description = "List of required signers for the transaction.")
    public List<Map<String, String>> getTxRequiredSigners(@PathVariable String hash) {
        return bfTransactionService.getTxRequiredSigners(hash);
    }
}
