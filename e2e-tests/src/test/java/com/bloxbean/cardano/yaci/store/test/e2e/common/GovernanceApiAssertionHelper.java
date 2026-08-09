package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalStatus;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.ProposalApiService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class GovernanceApiAssertionHelper {
    private final ProposalApiService proposalApiService;

    public GovernanceApiAssertionHelper(ProposalApiService proposalApiService) {
        this.proposalApiService = proposalApiService;
    }

    public ProposalDto assertLatestApiStatus(String txHash, int index, ProposalStatus expectedStatus) {
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(proposalApiService.getProposalById(txHash, index))
                        .hasValueSatisfying(proposal -> assertThat(proposal.getStatus()).isEqualTo(expectedStatus)));

        return proposalApiService.getProposalById(txHash, index).orElseThrow();
    }
}
