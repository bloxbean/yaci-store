package com.bloxbean.cardano.yaci.store.api.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalStatus;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.SpecialDRepDto;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
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
    private final EpochParamStorage epochParamStorage;

    public List<ProposalDto> getProposals(int page, int count, Order order) {
        List<GovActionProposal> proposals = proposalReader.findAll(page, count, order);

        Integer maxEpoch = epochParamStorage.getMaxEpoch();

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
                        proposalStatus = toProposalStatus(status, maxEpoch);
                        votingStats = status.getVotingStats();
                    }

                    return new ProposalDto(
                            p.getTxHash(),
                            (int) p.getIndex(),
                            p.getSlot(),
                            p.getDeposit(),
                            p.getReturnAddress(),
                            p.getDetails(),
                            p.getAnchorUrl(),
                            p.getAnchorHash(),
                            proposalStatus,
                            votingStats,
                            p.getEpoch(),
                            p.getBlockNumber(),
                            p.getBlockTime()
                    );
                })
                .toList();
    }

    public Optional<ProposalDto> getProposalById(String txHash, int index) {
        Optional<GovActionProposal> proposalOpt = proposalReader.findByGovActionTxHashAndGovActionIndex(txHash, index);
        if (proposalOpt.isEmpty())
            return Optional.empty();

        Integer maxEpoch = epochParamStorage.getMaxEpoch();

        GovActionProposal proposal = proposalOpt.get();

        GovActionId id = new GovActionId(txHash, index);
        List<GovActionProposalStatus> statusList = statusReader.findLatestStatusesForProposals(List.of(id));

        GovActionProposalStatus status = statusList.isEmpty() ? null : statusList.get(0);

        ProposalStatus proposalStatus = status == null ? ProposalStatus.LIVE : toProposalStatus(status, maxEpoch);
        ProposalVotingStats votingStats = status == null ? new ProposalVotingStats() : status.getVotingStats();

        ProposalDto dto = new ProposalDto(
                proposal.getTxHash(),
                (int) proposal.getIndex(),
                proposal.getSlot(),
                proposal.getDeposit(),
                proposal.getReturnAddress(),
                proposal.getDetails(),
                proposal.getAnchorUrl(),
                proposal.getAnchorHash(),
                proposalStatus,
                votingStats,
                proposal.getEpoch(),
                proposal.getBlockNumber(),
                proposal.getBlockTime()
        );

        return Optional.of(dto);
    }

    private ProposalStatus toProposalStatus(GovActionProposalStatus govActionProposalStatus, int currentEpoch) {
        if (govActionProposalStatus == null) return null;

        if (govActionProposalStatus.getStatus() == GovActionStatus.ACTIVE) {
            if (govActionProposalStatus.getEpoch() == currentEpoch || govActionProposalStatus.getEpoch() == currentEpoch - 1) {
                return ProposalStatus.LIVE;
            } else {
                return ProposalStatus.EXPIRED;
            }
        }

        if (govActionProposalStatus.getStatus() == GovActionStatus.EXPIRED) {
            return ProposalStatus.EXPIRED;
        }

        if (govActionProposalStatus.getStatus() == GovActionStatus.RATIFIED) {
            if (govActionProposalStatus.getEpoch() < currentEpoch) {
                return ProposalStatus.ENACTED;
            } else {
                return ProposalStatus.RATIFIED;
            }
        }

        return null;
    }
}
