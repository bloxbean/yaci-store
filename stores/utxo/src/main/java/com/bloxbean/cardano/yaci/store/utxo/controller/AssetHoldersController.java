package com.bloxbean.cardano.yaci.store.utxo.controller;

import com.bloxbean.cardano.yaci.store.utxo.domain.AssetHolder;
import com.bloxbean.cardano.yaci.store.utxo.service.UtxoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${apiPrefix}/assets")
public class AssetHoldersController {

    private final UtxoService utxoService;

    @GetMapping("{unit}/holders")
    public List<AssetHolder> getAssetHolders(@PathVariable String unit) {
        return utxoService.findAssetHoldersByUnit(unit, 0, 100) ;
    }
}
