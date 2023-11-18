package com.bloxbean.cardano.yaci.store.api.metadata.controller;

import com.bloxbean.cardano.yaci.store.api.metadata.dto.MetadataDtoMapper;
import com.bloxbean.cardano.yaci.store.api.metadata.dto.MetadataLabelDto;
import com.bloxbean.cardano.yaci.store.api.metadata.dto.TxMetadataLabelCBORDto;
import com.bloxbean.cardano.yaci.store.api.metadata.dto.TxMetadataLabelDto;
import com.bloxbean.cardano.yaci.store.api.metadata.service.MetadataService;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
public class MetadataController {
    private final MetadataService metadataService;
    private final MetadataDtoMapper metadataDtoMapper;

    @GetMapping("/txs/{txHash}/metadata")
    public List<TxMetadataLabelDto> getMetadataByTxHash(@PathVariable String txHash) {
        List<TxMetadataLabel> txMetadataLabels = metadataService.getMetadataForTx(txHash);
        if (txMetadataLabels == null || txMetadataLabels.isEmpty())
            return Collections.emptyList();
        else {
            return txMetadataLabels.stream()
                    .map(metadataDtoMapper::toTxMetadataLabelDto)
                    .toList();
        }
    }

    @GetMapping("/txs/{txHash}/metadata/cbor")
    public List<TxMetadataLabelCBORDto> getMetadataCborByTxHash(@PathVariable String txHash) {
        List<TxMetadataLabel> txMetadataLabels = metadataService.getMetadataForTx(txHash);
        if (txMetadataLabels == null || txMetadataLabels.isEmpty())
            return Collections.emptyList();
        else {
            return txMetadataLabels.stream()
                    .map(metadataDtoMapper::toTxMetadataLabelCBORDto)
                    .toList();
        }
    }

    @GetMapping("/metadata/txs/labels/{label}")
    public List<MetadataLabelDto> getMetadataByLabel(@PathVariable String label, @RequestParam(name = "page", defaultValue = "0") int page,
                                                     @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        if (count > 100)
            throw new IllegalArgumentException("Max no of records allowed is 100");

        return metadataService.getMetadataByLabel(label, p, count)
                .stream()
                .map(metadataDtoMapper::toMetadataLabelDto)
                .toList();
    }

}
