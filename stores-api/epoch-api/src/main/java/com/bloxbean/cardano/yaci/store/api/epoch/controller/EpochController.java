package com.bloxbean.cardano.yaci.store.api.epoch.controller;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.yaci.store.api.epoch.service.EpochParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Epoch Service")
@RequestMapping("${apiPrefix}/epochs")
@ConditionalOnExpression("${store.epoch.api-enabled:true} && ${store.epoch.enabled:true}")
public class EpochController {

    private final EpochParamService epochParamService;

    @GetMapping("latest/parameters")
    @Operation(summary = "Latest Epoch's Protocol Parameters", description = "Get the protocol parameters of the latest epoch.")
    public ProtocolParams getLatestProtocolParams() {
        return epochParamService.getLatestProtocolParams()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found"));
    }

    @GetMapping("{number}/parameters")
    @Operation(summary = "Specific Epoch's Protocol Parameters", description = "Get the protocol parameters of a specific epoch.")
    public ProtocolParams getProtocolParamsByEpochNo(@PathVariable Integer number) {
        return epochParamService.getProtocolParams(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found for epoch: " + number));
    }

    @GetMapping("latest")
    @Operation(summary = "Latest Epoch Information", description = "Get the information of the latest epoch.")
    public EpochContent getLatestEpoch() {
        return epochParamService.getLatestEpoch();
    }
}
