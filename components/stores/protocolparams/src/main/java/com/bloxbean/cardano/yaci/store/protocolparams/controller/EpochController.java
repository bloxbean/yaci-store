package com.bloxbean.cardano.yaci.store.protocolparams.controller;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.store.protocolparams.service.ProtocolParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/epochs")
@Slf4j
@ConditionalOnBean(LocalClientProvider.class)
public class EpochController {

    private final ProtocolParamService protocolParamService;

    public EpochController(ProtocolParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    //TODO -- This is a workaround for now. As we keep only the current protocol params now
    @GetMapping("/parameters")
    public ProtocolParams getProtocolParams() {
       return protocolParamService.getCurrentProtocolParams()
               .map(protocolParams -> protocolParams)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found"));
    }

}
