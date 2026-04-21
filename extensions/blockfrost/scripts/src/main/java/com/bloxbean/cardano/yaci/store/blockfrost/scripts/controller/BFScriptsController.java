package com.bloxbean.cardano.yaci.store.blockfrost.scripts.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.scripts.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.service.BFScriptsService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Scripts")
@RequestMapping("${blockfrost.apiPrefix}/scripts")
@ConditionalOnExpression("${store.extensions.blockfrost.scripts.enabled:false}")
public class BFScriptsController {

    private final BFScriptsService bfScriptsService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost ScriptsController initialized >>>");
    }

    // ── Endpoint 1: list all scripts ─────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Scripts", description = "List of scripts.")
    public List<BFScriptListItemDto> getScripts(
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfScriptsService.getScripts(p, count, order);
    }

    // ── Endpoint 2: script metadata ──────────────────────────────────────────

    @GetMapping("{script_hash}")
    @Operation(summary = "Specific script", description = "Information about a specific script.")
    public BFScriptDto getScript(@PathVariable("script_hash") String scriptHash) {
        validateScriptHash(scriptHash);
        return bfScriptsService.getScript(scriptHash);
    }

    // ── Endpoint 3: script JSON ──────────────────────────────────────────────

    @GetMapping("{script_hash}/json")
    @Operation(summary = "Script JSON", description = "JSON representation of a timelock script.")
    public BFScriptJsonDto getScriptJson(@PathVariable("script_hash") String scriptHash) {
        validateScriptHash(scriptHash);
        return bfScriptsService.getScriptJson(scriptHash);
    }

    // ── Endpoint 4: script CBOR ──────────────────────────────────────────────

    @GetMapping("{script_hash}/cbor")
    @Operation(summary = "Script CBOR", description = "CBOR representation of a Plutus script.")
    public BFScriptCborDto getScriptCbor(@PathVariable("script_hash") String scriptHash) {
        validateScriptHash(scriptHash);
        return bfScriptsService.getScriptCbor(scriptHash);
    }

    // ── Endpoint 5: script redeemers ─────────────────────────────────────────

    @GetMapping("{script_hash}/redeemers")
    @Operation(summary = "Redeemers of a specific script", description = "List of redeemers of a specific script.")
    public List<BFScriptRedeemerDto> getScriptRedeemers(
            @PathVariable("script_hash") String scriptHash,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        validateScriptHash(scriptHash);
        int p = page - 1;
        return bfScriptsService.getScriptRedeemers(scriptHash, p, count, order);
    }

    // ── Endpoint 6: datum JSON ────────────────────────────────────────────────

    @GetMapping("datum/{datum_hash}")
    @Operation(summary = "Datum value", description = "Query JSON value of a datum by its hash.")
    public BFDatumDto getDatum(@PathVariable("datum_hash") String datumHash) {
        validateDatumHash(datumHash);
        return bfScriptsService.getDatum(datumHash);
    }

    // ── Endpoint 7: datum CBOR ────────────────────────────────────────────────

    @GetMapping("datum/{datum_hash}/cbor")
    @Operation(summary = "Datum CBOR value", description = "Query CBOR serialised value of a datum by its hash.")
    public BFDatumCborDto getDatumCbor(@PathVariable("datum_hash") String datumHash) {
        validateDatumHash(datumHash);
        return bfScriptsService.getDatumCbor(datumHash);
    }

    // ── Validation helpers ────────────────────────────────────────────────────

    private void validateScriptHash(String scriptHash) {
        if (scriptHash == null || !scriptHash.matches("[0-9a-fA-F]{56}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid or malformed script_hash format.");
        }
    }

    private void validateDatumHash(String datumHash) {
        if (datumHash == null || !datumHash.matches("[0-9a-fA-F]{64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid or malformed datum_hash format.");
        }
    }
}
