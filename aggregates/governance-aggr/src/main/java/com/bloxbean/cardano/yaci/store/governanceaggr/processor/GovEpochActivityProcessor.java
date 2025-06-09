package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.domain.ProposalStatusCapturedEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.GovEpochActivityService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovEpochActivityEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @EventListener
    @Transactional
    public void handleProposalStatusCapturedEvent(ProposalStatusCapturedEvent event) {
        // Handle the logic after the proposal status evaluation is completed

        /*
         We assess dormant epochs at the epoch boundary, after ratifying and/or expirying proposals. In
         case where there are no proposals left at the epoch boundary, then the next epoch is considered
         dormant.
         */
        int epoch = event.getEpoch();
        int prevEpoch = event.getEpoch() - 1;

        if (eraService.getEraForEpoch(epoch).getValue() < Era.Conway.getValue()) {
            return;
        }

        jdbcTemplate.update("delete from gov_epoch_activity where epoch = :epoch",
                new MapSqlParameterSource().addValue("epoch", epoch));

        // first epoch in Conway era, it will be a dormant epoch
        if (eraService.getEraForEpoch(prevEpoch).getValue() == Era.Babbage.getValue()
                && eraService.getEraForEpoch(epoch).getValue() == Era.Conway.getValue()) {
            govEpochActivityService.saveGovEpochActivity(epoch, Boolean.TRUE, 1);
            return;
        }

        // get active proposals in proposal status snapshot
        List<GovActionProposal> activeProposalsInProposalStatusSnapshot =
                proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.ACTIVE, epoch);

        final boolean isCurrentEpochDormantEpoch = activeProposalsInProposalStatusSnapshot.isEmpty();

        Optional<GovEpochActivityEntity> prevGovEpochActivityEntity =
                govEpochActivityService.getGovEpochActivity(prevEpoch);

        int currentDormantEpochCount;

        if (isCurrentEpochDormantEpoch) {
            if (prevGovEpochActivityEntity.isPresent() && prevGovEpochActivityEntity.get().getDormant()) {
                // Both previous and current epochs are dormant → increment the count
                currentDormantEpochCount = prevGovEpochActivityEntity.get().getDormantEpochCount() + 1;
            } else {
                // Current epoch is dormant, but previous wasn't (or no data) → start from 1
                currentDormantEpochCount = 1;
            }
        } else {
            // Current epoch is not dormant → reset count to 0
            currentDormantEpochCount = 0;
        }

        govEpochActivityService.saveGovEpochActivity(epoch, isCurrentEpochDormantEpoch, currentDormantEpochCount);
    }
}
