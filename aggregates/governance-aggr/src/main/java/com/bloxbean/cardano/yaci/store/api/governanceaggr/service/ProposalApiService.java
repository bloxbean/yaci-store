package com.bloxbean.cardano.yaci.store.api.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalStatus;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProposalApiService {
    private final GovActionProposalStorageReader proposalReader;
    private final GovActionProposalStatusStorageReader statusReader;
    private final AdaPotJobStorage adaPotJobStorage;

    public List<ProposalDto> getProposals(int page, int count, Order order) {
        List<GovActionProposal> proposals = proposalReader.findAll(page, count, order);

        List<AdaPotJob> lastJob = adaPotJobStorage.getRecentCompletedJobs(1);
        Integer maxCompletedAdaPotJobEpoch = lastJob.isEmpty() ? null : lastJob.getFirst().getEpoch();

        if (maxCompletedAdaPotJobEpoch == null) {
            return proposals.stream()
                    .map(p -> buildProposalDto(p, ProposalStatus.LIVE, new ProposalVotingStats()))
                    .toList();
        }

        List<GovActionId> ids = proposals.stream()
                .map(p -> new GovActionId(p.getTxHash(), (int) p.getIndex()))
                .toList();

        List<GovActionProposalStatus> statuses = statusReader.findLatestStatusesForProposals(ids);

        Map<String, GovActionProposalStatus> statusMap = statuses.stream()
                .collect(Collectors.toMap(
                        s -> s.getGovActionTxHash() + "#" + s.getGovActionIndex(),
                        s -> s
                ));

        return proposals.stream()
                .map(p -> {
                    String key = p.getTxHash() + "#" + p.getIndex();
                    GovActionProposalStatus status = statusMap.get(key);
                    ProposalStatus proposalStatus;
                    ProposalVotingStats votingStats;

                    if (status == null) {
                        proposalStatus = ProposalStatus.LIVE;
                        votingStats = new ProposalVotingStats();
                    } else {
                        proposalStatus = toProposalStatus(status, maxCompletedAdaPotJobEpoch);
                        votingStats = status.getVotingStats();
                    }

                    return buildProposalDto(p, proposalStatus, votingStats);
                })
                .toList();
    }

    public Optional<ProposalDto> getProposalById(String txHash, int index) {
        Optional<GovActionProposal> proposalOpt = proposalReader.findByGovActionTxHashAndGovActionIndex(txHash, index);
        if (proposalOpt.isEmpty())
            return Optional.empty();

        List<AdaPotJob> lastJob = adaPotJobStorage.getRecentCompletedJobs(1);
        Integer maxCompletedAdaPotJobEpoch = lastJob.isEmpty() ? null : lastJob.getFirst().getEpoch();

        GovActionProposal proposal = proposalOpt.get();
        if (maxCompletedAdaPotJobEpoch == null) {
            return Optional.of(buildProposalDto(proposal, ProposalStatus.LIVE, new ProposalVotingStats()));
        }

        GovActionId id = new GovActionId(txHash, index);
        List<GovActionProposalStatus> statusList = statusReader.findLatestStatusesForProposals(List.of(id));

        GovActionProposalStatus status = statusList.isEmpty() ? null : statusList.get(0);

        ProposalStatus proposalStatus = status == null ? ProposalStatus.LIVE : toProposalStatus(status, maxCompletedAdaPotJobEpoch);
        ProposalVotingStats votingStats = status == null ? new ProposalVotingStats() : status.getVotingStats();

        ProposalDto dto = buildProposalDto(proposal, proposalStatus, votingStats);

        return Optional.of(dto);
    }

    private ProposalDto buildProposalDto(GovActionProposal proposal,
                                         ProposalStatus status,
                                         ProposalVotingStats votingStats) {

        return new ProposalDto(
                proposal.getTxHash(),
                (int) proposal.getIndex(),
                proposal.getSlot(),
                proposal.getDeposit(),
                proposal.getReturnAddress(),
                proposal.getDetails(),
                proposal.getAnchorUrl(),
                proposal.getAnchorHash(),
                status,
                votingStats,
                proposal.getEpoch(),
                proposal.getBlockNumber(),
                proposal.getBlockTime()
        );
    }

    private ProposalStatus toProposalStatus(GovActionProposalStatus govActionProposalStatus, int maxCompletedAdaPobJobEpoch) {
        if (govActionProposalStatus == null) return null;

        if (govActionProposalStatus.getStatus() == GovActionStatus.ACTIVE) {
            if (govActionProposalStatus.getEpoch() < maxCompletedAdaPobJobEpoch) {
                return ProposalStatus.DROPPED;
            } else {
                return ProposalStatus.LIVE;
            }
        }

        if (govActionProposalStatus.getStatus() == GovActionStatus.EXPIRED) {
            return ProposalStatus.EXPIRED;
        }

        if (govActionProposalStatus.getStatus() == GovActionStatus.RATIFIED) {
            if (govActionProposalStatus.getEpoch() < maxCompletedAdaPobJobEpoch) {
                return ProposalStatus.ENACTED;
            } else {
                return ProposalStatus.RATIFIED;
            }
        }

        return null;
    }
}
