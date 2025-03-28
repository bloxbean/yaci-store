package com.bloxbean.cardano.yaci.store.governance.service;

import com.bloxbean.cardano.yaci.core.model.governance.Drep;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.DRepStakeDistributionQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.DRepStakeDistributionQueryResult;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.annotation.LocalSupport;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.local.LocalClientProviderManager;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.governance.annotation.LocalDRepStake;
import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalDRepDistr;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalDRepDistrStorage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@LocalSupport
@LocalDRepStake
@ReadOnly(false)
@Slf4j
public class LocalDRepDistrService {
    private final LocalClientProviderManager localClientProviderManager;
    private final LocalDRepDistrStorage localDRepDistrStorage;
    private final EraService eraService;

    public LocalDRepDistrService(LocalClientProviderManager localClientProviderManager, LocalDRepDistrStorage localDRepDistrStorage,
                                 EraService eraService) {
        this.localClientProviderManager = localClientProviderManager;
        this.localDRepDistrStorage = localDRepDistrStorage;
        this.eraService = eraService;
    }

    @Getter
    private Era era;

    @EventListener
    public void blockEvent(BlockHeaderEvent blockHeaderEvent) {
        if (blockHeaderEvent.getMetadata().getEra() != null && blockHeaderEvent.getMetadata().getEra().value >= com.bloxbean.cardano.yaci.core.model.Era.Conway.value
                && (era == null || !blockHeaderEvent.getMetadata().getEra().name().equalsIgnoreCase(era.name()))) {
            era = Era.valueOf(blockHeaderEvent.getMetadata().getEra().name());
            log.info("Current era: {}", era.name());
            try {
                log.info("Fetching dRep stake distribution ...");
                fetchAndSetDRepDistr();
            } catch (Exception e) {
                log.error("Fetching dRep stake distribution failed", e);
            }
        }
    }

    @EventListener
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
        if (!epochChangeEvent.getEventMetadata().isSyncMode()) {
            return;
        }

        era = Era.valueOf(epochChangeEvent.getEra().name());
        if (era.getValue() >= Era.Conway.getValue()) {
            try {
                log.info("Epoch change event received. Fetching and updating dRep stake distribution");
                fetchAndSetDRepDistr();
            } catch (Exception e) {
                log.error("Fetching dRep stake distribution failed", e);
            }
        }
    }

    public synchronized void fetchAndSetDRepDistr() {
        Optional<Tuple<Tip, Integer>> epochAndTip = eraService.getTipAndCurrentEpoch();
        if (epochAndTip.isEmpty()) {
            log.error("Epoch is null. Cannot fetch dRep stake distribution");
            return;
        }

        Integer epoch = epochAndTip.get()._2;
        long slot = epochAndTip.get()._1.getPoint().getSlot();
        Optional<LocalClientProvider> localClientProvider = localClientProviderManager.getLocalClientProvider();

        try {
            var localStateQueryClient = localClientProvider.map(LocalClientProvider::getLocalStateQueryClient).orElse(null);
            if (localStateQueryClient == null) {
                log.info("LocalStateQueryClient is not initialized. Please check if n2c-node-socket-path or n2c-host is configured properly.");
                return;
            }

            //Try to release first before a new query to avoid stale data
            try {
                localStateQueryClient.release().block(Duration.ofSeconds(5));
            } catch (Exception e) {
                //Ignore the error
            }

            try {
                localStateQueryClient.acquire().block(Duration.ofSeconds(5));
            } catch (Exception e) {
                // Ignore the error
            }

            Mono<DRepStakeDistributionQueryResult> mono = localStateQueryClient.executeQuery(new DRepStakeDistributionQuery(era, List.of()));
            mono.doOnError(throwable ->
                            log.error("DRep stake distribution sync error {}", throwable.getMessage()))
                    .doFinally(
                            signalType -> localClientProviderManager.close(localClientProvider.get()))
                    .subscribe(dRepStakeDistributionQueryResult -> {
                        Map<Drep, BigInteger> dRepStakeDistrMap = dRepStakeDistributionQueryResult.getDRepStakeMap();
                        List<LocalDRepDistr> localDRepDistrList = new ArrayList<>();

                        dRepStakeDistrMap.forEach((drep, amount) -> {
                            String drepHash;
                            if (drep.getType() == DrepType.NO_CONFIDENCE) {
                                drepHash = DrepType.NO_CONFIDENCE.name();
                            } else if (drep.getType() == DrepType.ABSTAIN) {
                                drepHash = DrepType.ABSTAIN.name();
                            } else {
                                drepHash = drep.getHash();
                            }

                            localDRepDistrList.add(LocalDRepDistr.builder()
                                    .drepHash(drepHash)
                                    .drepType(drep.getType())
                                    .amount(amount)
                                    .epoch(epoch)
                                    .slot(slot)
                                    .build());
                        });

                        if (!localDRepDistrList.isEmpty()) {
                            localDRepDistrStorage.saveAll(localDRepDistrList);
                        }
                    });
        } catch (Exception e) {
            localClientProviderManager.close(localClientProvider.get());
        }
    }

}
