package com.bloxbean.cardano.yaci.store.api.metadata.controller;

import com.bloxbean.cardano.yaci.store.api.metadata.dto.MetadataDtoMapper;
import com.bloxbean.cardano.yaci.store.api.metadata.dto.MetadataLabelDto;
import com.bloxbean.cardano.yaci.store.api.metadata.dto.TxMetadataLabelCBORDto;
import com.bloxbean.cardano.yaci.store.api.metadata.dto.TxMetadataLabelDto;
import com.bloxbean.cardano.yaci.store.api.metadata.service.MetadataService;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${apiPrefix}")
@Tag(name = "Transaction Service")
@ConditionalOnExpression("${store.transaction.api-enabled:true} && ${store.metadata.enabled:true}")
public class MetadataController {

    private final MetadataService metadataService;
    private final MetadataDtoMapper metadataDtoMapper;

    @GetMapping("/txs/{txHash}/metadata")
    @Operation(summary = "Transaction Metadata Labels", description = "Get a list of metadata labels included in a specific transaction.")
    public List<TxMetadataLabelDto> getMetadataByTxHash(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash) {
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
    @Operation(summary = "Transaction Metadata CBOR", description = "Get a list of metadata CBOR included in a specific transaction.")
    public List<TxMetadataLabelCBORDto> getMetadataCborByTxHash(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash) {
        List<TxMetadataLabel> txMetadataLabels = metadataService.getMetadataForTx(txHash);
        if (txMetadataLabels == null || txMetadataLabels.isEmpty())
            return Collections.emptyList();
        else {
            return txMetadataLabels.stream()
                    .map(metadataDtoMapper::toTxMetadataLabelCBORDto)
                    .toList();
        }
    }

    @GetMapping("/txs/metadata/labels/{label}")
    @Operation(summary = "Metadata Labels", description = "Get a list of metadata Label included by a specific label.")
    public List<MetadataLabelDto> getMetadataByLabel(@PathVariable String label, @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        return metadataService.getMetadataByLabel(label, p, count)
                .stream()
                .map(metadataDtoMapper::toMetadataLabelDto)
                .toList();
    }

}
