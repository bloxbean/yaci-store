package com.bloxbean.cardano.yaci.store.protocolparams.controller;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.store.protocolparams.service.ProtocolParamService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("${apiPrefix}/epochs")
@Slf4j
@ConditionalOnBean(LocalClientProvider.class)
public class EpochController {

    private final ProtocolParamService protocolParamService;

    public EpochController(ProtocolParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    //TODO -- This is a workaround for now. As we keep only the current protocol params now
    @GetMapping("parameters")
    public ProtocolParams getProtocolParams() {
       return protocolParamService.getCurrentProtocolParams()
               .map(protocolParams -> protocolParams)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found"));
    }

    @Operation(summary = "Get protocol parameters. The {number} path variable is ignored. So any value can be passed. It always returns current protocol parameters")
    @GetMapping("{number}/parameters")
    public ProtocolParams getProtocolParams(@PathVariable Integer number) {
        return getProtocolParams();
    }

    @Operation(summary = "This is a dummy endpoint for now. It returns a hardcode value, 1")
    @GetMapping("latest")
    public EpochContent getLatestEpoch() {
        return EpochContent.builder()
                .epoch(1)
                .build();
    }

}
