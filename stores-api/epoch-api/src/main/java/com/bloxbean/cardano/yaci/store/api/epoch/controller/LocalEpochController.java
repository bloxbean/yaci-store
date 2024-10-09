package com.bloxbean.cardano.yaci.store.api.epoch.controller;

import com.bloxbean.cardano.yaci.store.api.epoch.dto.EpochNo;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.epoch.service.LocalEpochParamServiceReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("${apiPrefix}/epochs")
@Tag(name = "Local Epoch Service", description = "Get epoch params directly from local Cardano Node through n2c local query.")
@Slf4j
@ConditionalOnBean(LocalEpochParamServiceReader.class)
@ConditionalOnExpression("${store.epoch.endpoints.epoch.local.enabled:false}")
public class LocalEpochController {

    private final LocalEpochParamServiceReader protocolParamService;
    private final DomainMapper mapper = DomainMapper.INSTANCE;

    @PostConstruct
    public void postConstruct() {
        log.info("LocalEpochController initialized >>>");
    }

    public LocalEpochController(LocalEpochParamServiceReader protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    @GetMapping("parameters")
    @Operation(summary = "Latest Epoch's Protocol Parameters", description = "Get the protocol parameters of the latest epoch. It fetches the protocol parameters from the local node through n2c local query.")
    public ProtocolParamsDto getProtocolParams() {
        return protocolParamService.getCurrentProtocolParams()
                .map(mapper::toProtocolParamsDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found"));
    }

    @GetMapping("latest/parameters")
    @Operation(summary = "Latest Epoch's Protocol Parameters", description = "Get the protocol parameters of the latest epoch. It fetches the protocol parameters from the local node through n2c local query.")
    public ProtocolParamsDto getLatestProtocolParams() {
        return getProtocolParams();
    }

    @Operation(summary = "Get protocol parameters for an epoch if exists. It fetches the protocol parameters from the local node through n2c local query. If the protocol param doesn't exist for an old epoch, it return status code 404.")
    @GetMapping("{number}/parameters")
    public ProtocolParamsDto getProtocolParams(@PathVariable Integer number) {
        return protocolParamService.getProtocolParams(number)
                .map(mapper::toProtocolParamsDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found for epoch : " + number));
    }

    @Operation(summary = "Get the latest epoch no")
    @GetMapping("latest")
    public EpochNo getLatestEpoch() {
        return protocolParamService.getMaxEpoch()
                .map(EpochNo::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Latest epoch not found"));
    }
}
