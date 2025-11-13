package com.bloxbean.cardano.yaci.store.blockfrost.epoch.controller;

import com.bloxbean.cardano.yaci.store.api.epoch.service.EpochParamService;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
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
@Tag(name = "Blockfrost Epochs")
@RequestMapping("${blockfrost.apiPrefix}/epochs")
@ConditionalOnExpression("${store.extensions.blockfrost.epoch.enabled:true}")
public class BFEpochsController {

    private final EpochParamService epochParamService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost EpochController initialized >>>");
    }

    @GetMapping("latest/parameters")
    @Operation(summary = "Latest epoch protocol parameters", description = "Return the protocol parameters for the latest epoch.")
    public ProtocolParamsDto getLatestProtocolParams() {
        return epochParamService.getLatestProtocolParams()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested component has not been found."));
    }

    @GetMapping("{number}/parameters")
    @Operation(summary = "Specific Epoch's Protocol Parameters", description = "Return the protocol parameters for the epoch specified.")
    public ProtocolParamsDto getProtocolParamsByEpochNo(@PathVariable Integer number) {
        return epochParamService.getProtocolParams(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol parameters not found for epoch: " + number));
    }

}
