package com.bloxbean.cardano.yaci.store.blockfrost.metadata.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataCborDto;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataJsonDto;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataLabelDto;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.service.BFMetadataService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Metadata")
@RequestMapping("${blockfrost.apiPrefix}/metadata/txs")
@ConditionalOnExpression("${store.extensions.blockfrost.metadata.enabled:false}")
public class BFMetadataController {

    private final BFMetadataService bfMetadataService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost MetadataController initialized >>>");
    }

    @GetMapping("/labels")
    @Operation(summary = "Transaction metadata labels",
            description = "List of all used transaction metadata labels.")
    public List<BFMetadataLabelDto> getLabels(
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfMetadataService.getLabels(p, count, order);
    }

    @GetMapping("/labels/{label}")
    @Operation(summary = "Transaction metadata content in JSON",
            description = "Transaction metadata per label.")
    public List<BFMetadataJsonDto> getMetadataByLabel(
            @PathVariable String label,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        if (label == null || label.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid label parameter.");
        }
        int p = page - 1;
        return bfMetadataService.getMetadataByLabel(label, p, count, order);
    }

    @GetMapping("/labels/{label}/cbor")
    @Operation(summary = "Transaction metadata content in CBOR",
            description = "Transaction metadata per label in CBOR.")
    public List<BFMetadataCborDto> getMetadataCborByLabel(
            @PathVariable String label,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        if (label == null || label.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid label parameter.");
        }
        int p = page - 1;
        return bfMetadataService.getMetadataCborByLabel(label, p, count, order);
    }
}
