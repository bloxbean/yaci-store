package com.bloxbean.cardano.yaci.store.api.epoch.controller;

import com.bloxbean.cardano.yaci.store.api.epoch.dto.EpochDto;
import com.bloxbean.cardano.yaci.store.api.epoch.service.EpochParamService;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@ConditionalOnExpression("${store.epoch.endpoints.epoch.enabled:true}")
@ConditionalOnMissingBean(LocalEpochController.class)
public class EpochController {

    private final EpochParamService epochParamService;

    @PostConstruct
    public void postConstruct() {
        log.info("EpochController initialized >>>");
    }

    @GetMapping("latest/parameters")
    @Operation(summary = "Latest Epoch's Protocol Parameters", description = "Get the protocol parameters of the latest epoch.")
    public ProtocolParamsDto getLatestProtocolParams() {
        return epochParamService.getLatestProtocolParams()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found"));
    }

    @GetMapping("{number}/parameters")
    @Operation(summary = "Specific Epoch's Protocol Parameters", description = "Get the protocol parameters of a specific epoch.")
    public ProtocolParamsDto getProtocolParamsByEpochNo(@PathVariable Integer number) {
        return epochParamService.getProtocolParams(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found for epoch: " + number));
    }

    @GetMapping("latest")
    @Operation(summary = "Latest Epoch", description = "Get latest epoch.")
    public EpochDto getLatestEpoch() {
        return epochParamService.getEpochDetails(epochParamService.getLatestEpoch());
    }
}
