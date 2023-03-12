package com.bloxbean.cardano.yaci.store.script.controller;

import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.TxContractDetails;
import com.bloxbean.cardano.yaci.store.script.service.ScriptService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
public class ScriptController {
    private final ScriptService scriptService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/scripts/{scriptHash}")
    public Script getScriptByHash(@PathVariable String scriptHash) {
        return scriptService.getScriptByHash(scriptHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Script not found"));
    }

    @GetMapping("/txs/{txHash}/scripts")
    public List<TxContractDetails> getTxContractDetails(@PathVariable String txHash) {
        return scriptService.getTransactionScripts(txHash);
    }

    @GetMapping("/scripts/datum/{datumHash}")
    public JsonNode getDatumJsonByHash(@PathVariable String datumHash) {
        return scriptService.getDatumAsJson(datumHash)
                .map(datumJson -> {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.put("json_value", datumJson);
                    return objectNode;
                }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Datum not found"));
    }

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
