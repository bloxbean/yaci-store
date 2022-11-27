package com.bloxbean.cardano.yaci.indexer.protocolparams.controller;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.yaci.core.helpers.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.indexer.protocolparams.service.ProtocolParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/epochs")
@Slf4j
@ConditionalOnBean(LocalStateQueryClient.class)
public class EpochController {

    private final ProtocolParamService protocolParamService;

    public EpochController(ProtocolParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    //TODO -- This is a workaround for now. As we keep only the current protocol params now
    @GetMapping("/parameters")
    public Mono<ProtocolParams> getProtocolParams() {
       return protocolParamService.getCurrentProtocolParams()
               .map(protocolParams -> Mono.just(protocolParams))
               .orElse(notFound());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ProtocolParams> notFound() {
        return Mono.empty();
    }

}
