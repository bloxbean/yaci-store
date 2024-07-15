package com.bloxbean.cardano.yaci.store.governance.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.GovStateQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.GovStateResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.Proposal;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalGovActionProposalStatusEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalGovActionProposalStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
@Slf4j
public class LocalGovStateService {
    private final LocalClientProvider localClientProvider;
    private final LocalStateQueryClient localStateQueryClient;
    private final LocalGovActionProposalStatusRepository localGovActionProposalStatusRepository;

    public LocalGovStateService(LocalClientProvider localClientProvider, LocalGovActionProposalStatusRepository localGovActionProposalStatusRepository) {
        this.localClientProvider = localClientProvider;
        this.localStateQueryClient = localClientProvider.getLocalStateQueryClient();
        this.localGovActionProposalStatusRepository = localGovActionProposalStatusRepository;
        log.info("LocalGovActionStateService initialized >>>");
    }

    @Transactional
    public void fetchAndSetGovState() {
        getGovStateFromNode()
                .doOnError(throwable -> {
                    log.error("Local gov state sync error {}", throwable.getMessage());
                })
                .subscribe(govStateResult -> {
                    List<LocalGovActionProposalStatusEntity> entitiesToSave = new ArrayList<>();

                    List<GovActionId> expiredGovActions = govStateResult.getNextRatifyState().getExpiredGovActions();
                    List<GovActionId> enactedGovActions = govStateResult.getNextRatifyState().getEnactedGovActions();

                    expiredGovActions.forEach(govActionId -> entitiesToSave.add(buildLocalGovActionProposalEntity(govActionId, GovActionStatus.EXPIRED)));
                    enactedGovActions.forEach(govActionId -> entitiesToSave.add(buildLocalGovActionProposalEntity(govActionId, GovActionStatus.ENACTED)));

                    List<GovActionId> proposalsInNextRatify = govStateResult.getProposals().stream().map(Proposal::getGovActionId).toList();
                    proposalsInNextRatify.forEach(govActionId -> {
                        if (!expiredGovActions.contains(govActionId) && !enactedGovActions.contains(govActionId)) {
                            entitiesToSave.add(buildLocalGovActionProposalEntity(govActionId, GovActionStatus.ACTIVE));
                        }
                    });

                    localGovActionProposalStatusRepository.saveAll(entitiesToSave);
                });
    }

    public Mono<GovStateResult> getGovStateFromNode() {
        //Try to release first before a new query to avoid stale data
        try {
            localStateQueryClient.release().block(Duration.ofSeconds(5));
        } catch (Exception e) {
            //Ignore the error
        }

        try {
            localStateQueryClient.acquire().block(Duration.ofSeconds(5));
        } catch (Exception e) {
            //Ignore the error
        }

        return localStateQueryClient.executeQuery(new GovStateQuery(Era.Conway));
    }

    private LocalGovActionProposalStatusEntity buildLocalGovActionProposalEntity(GovActionId govActionId, GovActionStatus status) {
        return LocalGovActionProposalStatusEntity.builder()
                .govActionTxHash(govActionId.getTransactionId())
                .govActionIndex(govActionId.getGov_action_index())
                .status(status)
                .build();
    }

}
