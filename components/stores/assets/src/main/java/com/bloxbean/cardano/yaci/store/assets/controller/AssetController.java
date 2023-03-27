package com.bloxbean.cardano.yaci.store.assets.controller;


import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
public class AssetController {
    private final AssetService assetService;

    @GetMapping("/txs/{txHash}/assets")
    public List<TxAsset> getAssetsByTx(@PathVariable String txHash) {
        return assetService.getAssetsByTx(txHash);
    }

}
