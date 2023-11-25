package com.bloxbean.cardano.yaci.store.api.epoch.controller;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Network Service")
@RequestMapping("${apiPrefix}/network")
@ConditionalOnExpression("${store.network.api-enabled:true} && ${store.epoch.enabled:true}")
public class ParamProposalController {

    private final ProtocolParamsProposalStorageReader protocolParamsProposalReader;

    @GetMapping("/param-proposals")
    @Operation(summary = "Param Update Proposals", description = "Get all parameter update proposals submitted to the chain starting Shelley era.")
    public List<ProtocolParamsProposal> getProtocolParamProposals(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return protocolParamsProposalReader.getProtocolParamsProposals(p, count, Order.desc);
    }

}
