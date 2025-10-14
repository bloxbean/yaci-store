package com.bloxbean.cardano.yaci.store.cip139.utxo.controller;

import com.bloxbean.cardano.yaci.store.cip139.utxo.dto.UtxoDto;
import com.bloxbean.cardano.yaci.store.cip139.utxo.service.UtxoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "CIP-139 Utxos")
@RequestMapping("${cip139.apiPrefix}/utxos")
@ConditionalOnExpression("${extensions.cip139.utxos.enabled:true}")
public class UtxoController {

    private UtxoService utxoService;

    @PostConstruct
    public void postConstruct() {
        log.info("CIP-139 UtxoController initialized >>>");
    }

    @GetMapping("asset")
    @Operation(summary = "Get Utxos by Asset", description = "Get all UTxOs that contain some of the specified asset.")
    public UtxoDto getUtxosByAsset(@RequestParam(name = "asset_name") String assetName, @RequestParam(name = "minting_policy_hash") String mintingPolicyHash) {
        return utxoService.getUtxoByAsset(assetName, mintingPolicyHash);
    }

    @GetMapping("transaction_hash")
    @Operation(summary = "Get Utxos by Transaction Hash", description = "Get all UTxOs produced by the transaction.")
    public UtxoDto getUtxosByTransactionHash(String txnHash) {
        return utxoService.getUtxoByTransactionHash(txnHash);
    }

    @GetMapping("address")
    @Operation(summary = "Get Utxos by Address", description = "Get all the utxos given a address.")
    public UtxoDto getUtxosByAddress(@RequestParam(name = "address") String address) {
        return utxoService.getUtxoByAddress(address);
    }

    @GetMapping("payment_credential")
    @Operation(summary = "Get Utxos for a Payment Credential", description = "Get all UTxOs present at the addresses which use the payment credential.")
    public UtxoDto getUtxosByPaymentCredential(@RequestParam(name = "tag", defaultValue = "pubkey_hash") String tag, @RequestParam(name = "value") String paymentCredential ) {
        return utxoService.getUtxoByPaymentCredential(paymentCredential);
    }

    @GetMapping("stake_credential")
    @Operation(summary = "Get Utxos for a Stake Credential", description = "Get all UTxOs present at the addresses which use the stake credential.")
    public UtxoDto getUtxosByStakeCredential(@RequestParam(name = "reward_address") String rewardAddress) {
        return utxoService.getUtxoByStakeCredential(rewardAddress);
    }

}
