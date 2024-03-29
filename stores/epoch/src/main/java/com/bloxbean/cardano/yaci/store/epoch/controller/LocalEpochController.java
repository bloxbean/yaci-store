package com.bloxbean.cardano.yaci.store.epoch.controller;

import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.epoch.service.LocalProtocolParamService;
import io.swagger.v3.oas.annotations.Operation;
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
@Slf4j
@ConditionalOnBean(LocalProtocolParamService.class)
@ConditionalOnExpression("${store.epoch.endpoints.epoch.local.enabled:false}")
public class LocalEpochController {

    private final LocalProtocolParamService protocolParamService;
    private final DomainMapper mapper = DomainMapper.INSTANCE;

    @PostConstruct
    public void postConstruct() {
        log.info("LocalEpochController initialized >>>");
    }

    public LocalEpochController(LocalProtocolParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    //TODO -- This is a workaround for now. As we keep only the current protocol params now
    @GetMapping("parameters")
    public ProtocolParamsDto getProtocolParams() {
       return protocolParamService.getCurrentProtocolParams()
               .map(mapper::toProtocolParamsDto)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol params not found"));
    }

    @GetMapping("latest/parameters")
    public ProtocolParamsDto getLatestProtocolParams() {
        return getProtocolParams();
    }

    @Operation(summary = "Get protocol parameters. The {number} path variable is ignored. So any value can be passed. It always returns current protocol parameters")
    @GetMapping("{number}/parameters")
    public ProtocolParamsDto getProtocolParams(@PathVariable Integer number) {
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
