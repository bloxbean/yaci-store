package com.bloxbean.cardano.yaci.store.script.controller;

import com.bloxbean.cardano.yaci.store.script.dto.ScriptDto;
import com.bloxbean.cardano.yaci.store.script.dto.TxContractDetails;
import com.bloxbean.cardano.yaci.store.script.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/scripts")
@Slf4j
public class ScriptController {
    private ScriptService scriptService;

    public ScriptController(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @GetMapping("{scriptHash}")
    public ScriptDto getScriptByHash(@PathVariable String scriptHash) {
        return scriptService.getScriptByHash(scriptHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Script not found"));
    }

    @GetMapping("/tx/{txHash}")
    public List<TxContractDetails> getTxContractDetails(@PathVariable String txHash) {
        return scriptService.getTransactionScripts(txHash);
    }
}
