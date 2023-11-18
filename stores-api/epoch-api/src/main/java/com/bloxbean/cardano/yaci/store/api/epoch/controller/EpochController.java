package com.bloxbean.cardano.yaci.store.api.epoch.controller;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.yaci.store.api.epoch.service.EpochParamService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("${apiPrefix}/epochs")
@RequiredArgsConstructor
@Slf4j
public class EpochController {
    private final EpochParamService epochParamService;

    @GetMapping("latest/parameters")
    public ProtocolParams getProtocolParams() {
        return epochParamService.getLatestProtocolParams()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found"));
    }

    @Operation(summary = "Get protocol parameters. The {number} path variable is ignored. So any value can be passed. It always returns current protocol parameters")
    @GetMapping("{number}/parameters")
    public ProtocolParams getProtocolParams(@PathVariable Integer number) {
        return epochParamService.getProtocolParams(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found for epoch: " + number));
    }

    @Operation(summary = "Get latest epoch")
    @GetMapping("latest")
    public EpochContent getLatestEpoch() {
        return epochParamService.getLatestEpoch();
    }
}
