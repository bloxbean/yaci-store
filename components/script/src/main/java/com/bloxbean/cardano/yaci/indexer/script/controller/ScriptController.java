package com.bloxbean.cardano.yaci.indexer.script.controller;

import com.bloxbean.cardano.yaci.indexer.script.model.ScriptDto;
import com.bloxbean.cardano.yaci.indexer.script.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public Mono<ScriptDto> getTransaction(@PathVariable String scriptHash) {
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
}
