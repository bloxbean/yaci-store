package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.GovEpochActivityService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovEpochActivityEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrConfiguration.STORE_GOVERNANCEAGGR_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(value = STORE_GOVERNANCEAGGR_ENABLED, defaultValue = false)
@Slf4j
public class GovEpochActivityProcessor {
    private final GovEpochActivityService govEpochActivityService;
    private final ProposalStateClient proposalStateClient;
    private final EraService eraService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final GovActionProposalStorage govActionProposalStorage;

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        int epoch = event.getEpoch();
        int prevEpoch = event.getEpoch() - 1;

        if (eraService.getEraForEpoch(epoch).getValue() < Era.Conway.getValue()) {
            return;
        }

        // first epoch in Conway era
        if (eraService.getEraForEpoch(prevEpoch).getValue() == Era.Babbage.getValue()
                && eraService.getEraForEpoch(epoch).getValue() == Era.Conway.getValue()) {
            govEpochActivityService.saveGovEpochActivity(epoch, Boolean.TRUE, 1);
            return;
        }

        jdbcTemplate.update("delete from gov_epoch_activity where epoch = :epoch",
                new MapSqlParameterSource().addValue("epoch", epoch));

        // get active proposals in prev proposal status snapshot (the epoch = current epoch - 1)
        List<GovActionProposal> activeProposalsInPrevProposalStatusSnapshot =
                proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.ACTIVE, prevEpoch);

        List<com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal> newProposalsCreatedInPrevEpoch =
                govActionProposalStorage.findByEpoch(prevEpoch)
                        .stream().sorted(Comparator.comparingLong(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal::getSlot)
                                .thenComparingLong(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal::getIndex))
                        .toList();

        boolean isDormantEpoch = activeProposalsInPrevProposalStatusSnapshot.isEmpty() && newProposalsCreatedInPrevEpoch.isEmpty();

        Optional<GovEpochActivityEntity> prevGovEpochActivityEntity =
                govEpochActivityService.getGovEpochActivity(prevEpoch);

        Integer dormantEpochCount = prevGovEpochActivityEntity
                .map(prevGovEpochActivity -> (prevGovEpochActivity.getDormant().equals(Boolean.TRUE) && isDormantEpoch)
                        ? prevGovEpochActivity.getDormantEpochCount() + 1
                        : (isDormantEpoch ? 1 : 0))
                .orElse(isDormantEpoch ? 1 : 0);

        govEpochActivityService.saveGovEpochActivity(epoch, isDormantEpoch, dormantEpochCount);
    }
}
