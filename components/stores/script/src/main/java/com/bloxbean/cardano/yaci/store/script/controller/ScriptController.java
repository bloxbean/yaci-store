package com.bloxbean.cardano.yaci.store.script.controller;

import com.bloxbean.cardano.yaci.store.script.dto.ScriptDto;
import com.bloxbean.cardano.yaci.store.script.dto.TxContractDetails;
import com.bloxbean.cardano.yaci.store.script.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/scripts")
@Slf4j
public class ScriptController {
    private ScriptService scriptService;

    public ScriptController(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @GetMapping("{scriptHash}")
    public Mono<ScriptDto> getScriptByHash(@PathVariable String scriptHash) {
        Optional<ScriptDto> scriptOptional = scriptService.getScriptByHash(scriptHash);
        if (scriptOptional.isPresent())
            return Mono.just(scriptOptional.get());
        else
            return notFound();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ScriptDto> notFound() {
        return Mono.empty();
    }

    @GetMapping("/tx/{txHash}")
    public List<TxContractDetails> getTxContractDetails(@PathVariable String txHash) {
        return scriptService.getTransactionScripts(txHash);
    }
}
