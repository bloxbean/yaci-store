package com.bloxbean.cardano.yaci.store.api.script.controller;

import com.bloxbean.cardano.yaci.store.api.script.dto.*;
import com.bloxbean.cardano.yaci.store.api.script.service.ScriptService;
import com.bloxbean.cardano.yaci.store.script.domain.TxContractDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${apiPrefix}")
public class ScriptController {

    private final ScriptService scriptService;
    private final ScriptDtoMapper scriptDtoMapper;
    private final ObjectMapper objectMapper;

    @Tag(name = "Script Service")
    @GetMapping("/scripts/{scriptHash}/details")
    public ScriptDetailDto getScriptDetailsByHash(@PathVariable String scriptHash) {
        return scriptService.getScriptByHash(scriptHash)
                .map(scriptDtoMapper::toScriptDetailDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Script not found"));
    }

    @Tag(name = "Script Service")
    @GetMapping("/scripts/{scriptHash}")
    public ScriptDto getScriptByHash(@PathVariable String scriptHash) {
        return scriptService.getScriptByHash(scriptHash)
                .map(scriptDtoMapper::toScriptDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Script not found"));
    }

    @Tag(name = "Script Service")
    @GetMapping("/scripts/{scriptHash}/cbor")
    public ScriptCborDto getScriptCborByHash(@PathVariable String scriptHash) {
        return scriptService.getScriptByHash(scriptHash)
                .map(scriptDtoMapper::toScriptCbor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Script not found"));
    }

    @Tag(name = "Script Service")
    @GetMapping("/scripts/{scriptHash}/json")
    public ScriptJsonDto getScriptJsonByHash(@PathVariable String scriptHash) {
        return scriptService.getScriptByHash(scriptHash)
                .map(scriptDtoMapper::toScriptJson)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Script not found"));
    }

    @Tag(name = "Transaction Service")
    @GetMapping("/txs/{txHash}/scripts")
    public List<TxContractDetails> getTxContractDetails(@PathVariable String txHash) {
        return scriptService.getTransactionScripts(txHash);
    }

    @Tag(name = "Transaction Service")
    @GetMapping("/txs/{txHash}/redeemers")
    public List<TxRedeemerDto> getTxRedeemers(@PathVariable String txHash) {
        return scriptService.getTransactionRedeemers(txHash);
    }

    @Tag(name = "Script Service")
    @GetMapping("/scripts/datum/{datumHash}")
    public JsonNode getDatumJsonByHash(@PathVariable String datumHash) {
        return scriptService.getDatumAsJson(datumHash)
                .map(datumJson -> {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.put("json_value", datumJson);
                    return objectNode;
                }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Datum not found"));
    }

    @Tag(name = "Script Service")
    @GetMapping("/scripts/datum/{datumHash}/cbor")
    public JsonNode getDatumCborByHash(@PathVariable String datumHash) {
        return scriptService.getDatum(datumHash)
                .map(datum -> {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.put("cbor", datum.getDatum());
                    return objectNode;
                }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Datum not found"));
    }
}
