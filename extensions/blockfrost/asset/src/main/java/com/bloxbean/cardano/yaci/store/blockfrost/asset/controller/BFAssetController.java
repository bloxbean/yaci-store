package com.bloxbean.cardano.yaci.store.blockfrost.asset.controller;


import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetAddressDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetDetailDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetHistoryDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.dto.BFAssetTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.service.BFAssetService;
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
@Tag(name = "Blockfrost Assets")
@RequestMapping("${blockfrost.apiPrefix}/assets")
@ConditionalOnExpression("${store.extensions.blockfrost.asset.enabled:false}")
public class BFAssetController {

    private final BFAssetService bfAssetService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost AssetController initialized >>>");
    }

    @GetMapping
    public List<BFAssetDTO> getAssets(@RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                      @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                      @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;

        return bfAssetService.getAssets(p, count, order);
    }

    @GetMapping("{asset}")
    @Operation(summary = "Specific asset", description = "Information about a specific asset")
    public BFAssetDetailDTO getAsset(@PathVariable String asset) {
        return bfAssetService.getAsset(asset);
    }

    @GetMapping("{asset}/history")
    @Operation(summary = "Asset history", description = "History of a specific asset")
    public List<BFAssetHistoryDTO> getAssetHistory(@PathVariable String asset,
                                                   @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                   @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                                   @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAssetService.getAssetHistory(asset, p, count, order);
    }

    @GetMapping("{asset}/txs")
    @Operation(summary = "Asset txs", description = "List of a specific asset transaction hashes")
    public List<String> getAssetTxs(@PathVariable String asset,
                                    @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                    @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                    @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAssetService.getAssetTxs(asset, p, count, order);
    }

    @GetMapping("{asset}/transactions")
    @Operation(summary = "Asset transactions", description = "List of transactions for a specific asset")
    public List<BFAssetTransactionDTO> getAssetTransactions(@PathVariable String asset,
                                                            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                                            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAssetService.getAssetTransactions(asset, p, count, order);
    }

    @GetMapping("{asset}/addresses")
    @Operation(summary = "Asset addresses", description = "List of addresses containing a specific asset")
    public List<BFAssetAddressDTO> getAssetAddresses(@PathVariable String asset,
                                                     @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                                     @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                                     @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAssetService.getAssetAddresses(asset, p, count, order);
    }

    @GetMapping("policy/{policy_id}")
    @Operation(summary = "Assets of a specific policy", description = "List of assets minted under a specific policy")
    public List<BFAssetDTO> getPolicyAssets(@PathVariable("policy_id") String policyId,
                                            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
                                            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
                                            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAssetService.getPolicyAssets(policyId, p, count, order);
    }
}
