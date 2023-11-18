package com.bloxbean.cardano.yaci.store.api.epoch.controller;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}/epoch/param-propsals")
@RequiredArgsConstructor
@Slf4j
public class ParamProposalController {
    private final ProtocolParamsProposalStorageReader protocolParamsProposalReader;

    @GetMapping
    public List<ProtocolParamsProposal> getProtocolParamProposals(@RequestParam(name = "page", defaultValue = "0") int page,
                                                        @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return protocolParamsProposalReader.getProtocolParamsProposals(p, count, Order.desc);
    }

}
