package com.bloxbean.cardano.yaci.store.script.controller;

import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.TxContractDetails;
import com.bloxbean.cardano.yaci.store.script.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping
@Slf4j
public class ScriptController {
    private ScriptService scriptService;

    public ScriptController(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @GetMapping("/scripts/{scriptHash}")
    public Script getScriptByHash(@PathVariable String scriptHash) {
        return scriptService.getScriptByHash(scriptHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Script not found"));
    }

    @GetMapping("/txs/{txHash}/scripts")
    public List<TxContractDetails> getTxContractDetails(@PathVariable String txHash) {
        return scriptService.getTransactionScripts(txHash);
    }
}
