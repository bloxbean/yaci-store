package com.bloxbean.cardano.yaci.store.metadata.controller;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadata;
import com.bloxbean.cardano.yaci.store.metadata.service.MetadataService;
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
public class MetadataController {
    private final MetadataService metadataService;

    @GetMapping("/txs/{txHash}/metadata")
    public List<TxMetadata> getMetadataByTxHash(@PathVariable String txHash) {
        return metadataService.getMetadataForTx(txHash);
    }

}
