package com.bloxbean.cardano.yaci.store.metadata.controller;

import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.dto.MetadataDtoMapper;
import com.bloxbean.cardano.yaci.store.metadata.dto.TxMetadataLabelCBORDto;
import com.bloxbean.cardano.yaci.store.metadata.dto.TxMetadataLabelDto;
import com.bloxbean.cardano.yaci.store.metadata.service.MetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
