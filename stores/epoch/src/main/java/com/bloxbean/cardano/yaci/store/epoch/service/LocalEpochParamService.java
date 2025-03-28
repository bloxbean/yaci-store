package com.bloxbean.cardano.yaci.store.epoch.service;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamQueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamsQuery;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.annotation.LocalSupport;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.local.LocalClientProviderManager;
import com.bloxbean.cardano.yaci.store.epoch.annotation.LocalEpochParam;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.LocalEpochParamsStorage;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Component
@LocalSupport
@LocalEpochParam
@ReadOnly(false)
@Slf4j
public class LocalEpochParamService {
    private final LocalClientProviderManager localClientProviderManager;
    private final EraService eraService;
    private final LocalEpochParamsStorage localProtocolParamsStorage;

    private DomainMapper domainMapper = DomainMapper.INSTANCE;

    @Getter
    private Era era;

    public LocalEpochParamService(LocalClientProviderManager localClientProviderManager,
                                  LocalEpochParamsStorage localProtocolParamsStorage, EraService eraService) {
        this.localClientProviderManager = localClientProviderManager;
        this.localProtocolParamsStorage = localProtocolParamsStorage;
        this.eraService = eraService;
        log.info("ProtocolParamService initialized >>>");
    }


    /**
     * Listen to block event to set the correct era
     * @param blockHeaderEvent
     */
    @EventListener
    public void blockEvent(BlockHeaderEvent blockHeaderEvent) {
        if (blockHeaderEvent.getMetadata().getEra() != null && blockHeaderEvent.getMetadata().getEra().value >= com.bloxbean.cardano.yaci.core.model.Era.Conway.value
                &&  (era == null || !blockHeaderEvent.getMetadata().getEra().name().equalsIgnoreCase(era.name()))) {
            era = Era.valueOf(blockHeaderEvent.getMetadata().getEra().name());
            log.info("Current era: {}", era.name());

            //Looks like era change, fetch protocol params
            //This is required for custom network directly starting from latest era like Conway era. So, after first block, when correct era is detected
            //fetch protocol params.
            log.info("Fetching protocol params ...");
            fetchAndSetCurrentProtocolParams();
        }
    }

    /**
     * Listen to epoch change event and fetch protocol param
     * @param epochChangeEvent
     */
    @EventListener
    public void epochEvent(EpochChangeEvent epochChangeEvent) {
        if (!epochChangeEvent.getEventMetadata().isSyncMode())
            return;

        era = Era.valueOf(epochChangeEvent.getEra().name());
        if (era.getValue() >= Era.Conway.value) {
            log.info("Epoch change event received. Fetching protocol params ...");
            fetchAndSetCurrentProtocolParams();
        }
    }

    public synchronized void fetchAndSetCurrentProtocolParams() {
        Optional<Tuple<Tip, Integer>> epochAndTip = eraService.getTipAndCurrentEpoch();
        if (epochAndTip.isEmpty()) {
            log.error("Epoch is null. Cannot fetch protocol params");
            return;
        }

        Integer epoch = epochAndTip.get()._2;
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

            Mono<CurrentProtocolParamQueryResult> mono = localStateQueryClient.executeQuery(new CurrentProtocolParamsQuery(era));
            mono.map(CurrentProtocolParamQueryResult::getProtocolParams)
                    .doOnError(throwable ->
                            log.error("Protocol param sync error {}", throwable.getMessage()))
                    .doFinally(
                            signalType -> localClientProviderManager.close(localClientProvider.get()))
                    .subscribe(protocolParamUpdate -> {
                        EpochParam epochParam = new EpochParam();
                        epochParam.setEpoch(epoch);
                        epochParam.setParams(convertProtoParams(protocolParamUpdate));
                        localProtocolParamsStorage.save(epochParam);
                    });
        } catch (Exception e) {
            localClientProviderManager.close(localClientProvider.get());
        }
    }

    public Optional<ProtocolParams> getCurrentProtocolParams() {
        return localProtocolParamsStorage.getLatestEpochParam()
                .map(EpochParam::getParams);
    }

    public Optional<ProtocolParams> getProtocolParams(int epoch) {
        return localProtocolParamsStorage.getEpochParam(epoch)
                .map(EpochParam::getParams);
    }

    public Optional<Integer> getMaxEpoch() {
        return localProtocolParamsStorage.getMaxEpoch();
    }

    private ProtocolParams convertProtoParams(ProtocolParamUpdate protocolParamUpdate) {
        return domainMapper.toProtocolParams(protocolParamUpdate);
    }

}
