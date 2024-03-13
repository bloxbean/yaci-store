package com.bloxbean.cardano.yaci.store.api.utxo.controller;

import com.bloxbean.cardano.yaci.store.api.utxo.service.AssetService;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("utxoAssetController")
@Tag(name = "Asset Service")
@RequestMapping("${apiPrefix}/assets")
@ConditionalOnExpression("${store.utxo.endpoints.asset.enabled:true}")
public class AssetController {

    private final AssetService assetService;

    @Autowired
    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping("/utxos/unit/{unit}")
    @Operation(summary = "Asset UTxOs by Unit", description = "Get all UTxOs information of an asset.")
    public List<Utxo> getAssetUtxos(@PathVariable String unit, @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                    @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count,
                                    @RequestParam(required = false, defaultValue = "asc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return assetService.getUtxosByAsset(unit, p, count, order);
    }
}
