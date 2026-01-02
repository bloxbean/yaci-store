package com.bloxbean.cardano.yaci.store.api.governanceaggr.controller;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.processor.ProposalStateProcessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("DebugProposalController")
@RequestMapping("${apiPrefix}/debug/governance-state")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Proposal Debug API", description = "APIs for proposal related data.")
@Profile({"debug"})
public class ProposalStatusDebugController {
    private final ProposalStateProcessor proposalStateProcessor;

    @GetMapping("/proposal-status/{epoch}")
    public List<GovActionProposalStatus> processProposalStatus(@PathVariable int epoch) {
        log.info("Debug proposal status processing requested for epoch: {}", epoch);
        return proposalStateProcessor.getProposalStatuses(epoch);
    }
}
