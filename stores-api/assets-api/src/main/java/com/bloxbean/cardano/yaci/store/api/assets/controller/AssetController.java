package com.bloxbean.cardano.yaci.store.api.assets.controller;

import com.bloxbean.cardano.yaci.store.api.assets.service.AssetService;
import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Asset Service")
@RequestMapping("${apiPrefix}/assets")
@ConditionalOnExpression("${store.assets.endpoints.asset.enabled:true}")
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/txs/{txHash}")
    @Operation(summary = "Assets History by Tx Hash", description = "Returns the Mint / Burn History for all assets included in a transaction.")
    public List<TxAsset> getAssetTxsByTx(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash) {
        return assetService.getAssetsByTx(txHash);
    }

    @GetMapping("/fingerprint/{fingerprint}")
    @Operation(summary = "Asset History by Fingerprint", description = "Returns the Mint / Burn History of an asset by fingerprint.")
    public List<TxAsset> getAssetTxsByFingerprint(@PathVariable String fingerprint, @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return assetService.getAssetTxsByFingerprint(fingerprint, p, count);
    }

    @GetMapping("/unit/{unit}")
    @Operation(summary = "Asset History by Unit", description = "Returns the Mint / Burn History of an asset by unit.")
    public List<TxAsset> getAssetTxsByUnit(@PathVariable String unit, @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                           @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return assetService.getAssetTxsByUnit(unit, p, count);
    }

    @GetMapping("/policy/{policyId}")
    @Operation(summary = "Asset History by Policy", description = "Returns the Mint / Burn History for all assets included in a policy.")
    public List<TxAsset> getAssetTxsByPolicyId(@PathVariable String policyId, @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                             @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return assetService.getAssetTxsByPolicyId(policyId, p, count);
    }

    @GetMapping("/supply/fingerprint/{fingerprint}")
    @Operation(summary = "Assets Supply by Fingerprint", description = "Returns the entire supply of a specific asset by fingerprint.")
    public FingerprintSupply getSupplyByFingerprint(@PathVariable String fingerprint) {
        return assetService.getSupplyByFingerprint(fingerprint)
                .map(supply -> new FingerprintSupply(fingerprint, supply))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    }

    @GetMapping("/supply/unit/{unit}")
    @Operation(summary = "Assets Supply by Unit", description = "Returns the entire supply of a specific asset by unit.")
    public UnitSupply getSupplyByUnit(@PathVariable String unit) {
        return assetService.getSupplyByUnit(unit)
                .map(supply -> new UnitSupply(unit, supply))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    }

    @GetMapping("/supply/policy/{policy}")
    @Operation(summary = "Assets Supply by Policy", description = "Returns the entire assets supply of a specific policy.")
    public PolicySupply getSupplyByPolicy(@PathVariable String policy) {
        return assetService.getSupplyByPolicy(policy)
                .map(supply -> new PolicySupply(policy, supply))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FingerprintSupply {

        private String fingerprint;
        private BigInteger supply;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitSupply {

        private String unit;
        private BigInteger supply;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicySupply {

        private String policy;
        private BigInteger supply;
    }
}
