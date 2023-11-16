package com.bloxbean.cardano.yaci.store.api.assets.controller;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.api.assets.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
public class AssetController {
    private final AssetService assetService;

    @GetMapping("/txs/{txHash}/assets")
    public List<TxAsset> getAssetTxsByTx(@PathVariable String txHash) {
        return assetService.getAssetsByTx(txHash);
    }

    @GetMapping("/assets/txs/fingerprint/{fingerprint}")
    public List<TxAsset> getAssetTxsByFingerprint(@PathVariable String fingerprint, @RequestParam(name = "page", defaultValue = "0") int page,
                                                @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        if (count > 100)
            throw new IllegalArgumentException("Max no of records allowed is 100");

        return assetService.getAssetTxsByFingerprint(fingerprint, page, count);
    }

    @GetMapping("/assets/txs/policy/{policyId}")
    public List<TxAsset> getAssetTxsByPolicyId(@PathVariable String policyId, @RequestParam(name = "page", defaultValue = "0") int page,
                                             @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        if (count > 100)
            throw new IllegalArgumentException("Max no of records allowed is 100");

        return assetService.getAssetTxsByPolicyId(policyId, page, count);
    }

    @GetMapping("/assets/txs/unit/{unit}")
    public List<TxAsset> getAssetTxsByUnit(@PathVariable String unit, @RequestParam(name = "page", defaultValue = "0") int page,
                                         @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        if (count > 100)
            throw new IllegalArgumentException("Max no of records allowed is 100");

        return assetService.getAssetTxsByUnit(unit, page, count);
    }

    @GetMapping("/assets/supply/fingerprint/{fingerprint}")
    public FingerprintSupply getSupplyByFingerprint(@PathVariable String fingerprint) {
        return assetService.getSupplyByFingerprint(fingerprint)
                .map(supply -> new FingerprintSupply(fingerprint, supply))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    }

    @GetMapping("/assets/supply/unit/{unit}")
    public UnitSupply getSupplyByUnit(@PathVariable String unit) {
        return assetService.getSupplyByUnit(unit)
                .map(supply -> new UnitSupply(unit, supply))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    }

    @GetMapping("/assets/supply/policy/{policy}")
    public PolicySupply getSupplyByPolicy(@PathVariable String policy) {
        return assetService.getSupplyByPolicy(policy)
                .map(supply -> new PolicySupply(policy, supply))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    }


    record FingerprintSupply(String fingerprint, int supply) {
    }

    record UnitSupply(String unit, int supply) {
    }

    record PolicySupply(String policy, int supply) {
    }
}
