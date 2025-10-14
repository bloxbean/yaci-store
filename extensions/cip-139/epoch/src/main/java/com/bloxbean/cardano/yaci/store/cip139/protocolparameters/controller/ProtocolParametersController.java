package com.bloxbean.cardano.yaci.store.cip139.protocolparameters.controller;

import com.bloxbean.cardano.yaci.store.cip139.protocolparameters.dto.ProtocolParametersDto;
import com.bloxbean.cardano.yaci.store.cip139.protocolparameters.service.ProtocolParametersService;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "CIP-139 Protocol Parameters")
@RequestMapping("${cip139.apiPrefix}/protocol_parameters")
@ConditionalOnExpression("${store.extensions.cip139.protocolparameters.enabled:true}")
public class ProtocolParametersController {

    private final ProtocolParametersService protocolParametersService;

    @PostConstruct
    public void postConstruct() {
        log.info("CIP-139 EpochController initialized >>>");
    }

    @GetMapping("latest")
    @Operation(summary = "Latest Epoch's Protocol Parameters", description = "Get the protocol parameters of the latest epoch.")
    public ProtocolParametersDto getLatestProtocolParams() {
        return protocolParametersService.getLatestProtocolParams()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found"));
    }

    @GetMapping("epoch")
    @Operation(summary = "Specific Epoch's Protocol Parameters", description = "Get the protocol parameters of a specific epoch.")
    public ProtocolParametersDto getProtocolParamsByEpochNo(@RequestParam(name = "u_int32") Integer epoch) {
        return protocolParametersService.getProtocolParams(epoch)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found for epoch: " + epoch));
    }

}
