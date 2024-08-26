package com.bloxbean.cardano.yaci.store.epoch.service;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamQueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamsQuery;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.LocalEpochParamsStorage;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
@ConditionalOnProperty(
        prefix = "store.epoch",
        name = "n2c-protocol-param-enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class LocalEpochParamService {
    private final LocalClientProvider localClientProvider;
    private final LocalStateQueryClient localStateQueryClient;
    private final EraService eraService;
    private final LocalEpochParamsStorage localProtocolParamsStorage;

    private DomainMapper domainMapper = DomainMapper.INSTANCE;

    @Value("${store.cardano.n2c-era:Conway}")
    private String eraStr;

    private Era era;

    public LocalEpochParamService(LocalClientProvider localClientProvider, LocalEpochParamsStorage localProtocolParamsStorage, EraService eraService) {
        this.localClientProvider = localClientProvider;
        this.localStateQueryClient = localClientProvider.getLocalStateQueryClient();
        this.localProtocolParamsStorage = localProtocolParamsStorage;
        this.eraService = eraService;
        log.info("ProtocolParamService initialized >>>");
    }

    @PostConstruct
    public void postConstruct() {
        if (StringUtil.isEmpty(eraStr))
            eraStr = "Conway";

        era = Era.valueOf(eraStr);
        log.info("N2C Era set to {}", era.name());
    }

    /**
     * Listen to block event to set the correct era
     * @param blockHeaderEvent
     */
    @EventListener
    public void blockEvent(BlockHeaderEvent blockHeaderEvent) {
        if (!blockHeaderEvent.getMetadata().isSyncMode())
            return;

        if (blockHeaderEvent.getMetadata().getEra() != null
                && !blockHeaderEvent.getMetadata().getEra().name().equalsIgnoreCase(era.name())) {
            era = Era.valueOf(blockHeaderEvent.getMetadata().getEra().name());
            log.info("Era changed to {}", era.name());

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

        log.info("Epoch change event received. Fetching protocol params ...");
        fetchAndSetCurrentProtocolParams();
    }

    @Transactional
    public synchronized void fetchAndSetCurrentProtocolParams() {
        Optional<Tuple<Tip,Integer>> epochAndTip = eraService.getTipAndCurrentEpoch();
        if (epochAndTip.isEmpty()) {
            log.error("Epoch is null. Cannot fetch protocol params");
            return;
        }

        Integer epoch = epochAndTip.get()._2;

        var protocolParamUpdate = getCurrentProtocolParamsFromNode().block(Duration.ofSeconds(5));

        EpochParam epochParam = new EpochParam();
        epochParam.setEpoch(epoch);
        epochParam.setParams(convertProtoParams(protocolParamUpdate));
        localProtocolParamsStorage.save(epochParam);
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

    public Mono<ProtocolParamUpdate> getCurrentProtocolParamsFromNode() {
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

        Mono<CurrentProtocolParamQueryResult> mono =
                localStateQueryClient.executeQuery(new CurrentProtocolParamsQuery(era));
        return mono.map(currentProtocolParamQueryResult -> currentProtocolParamQueryResult.getProtocolParams());
    }

    private ProtocolParams convertProtoParams(ProtocolParamUpdate protocolParamUpdate) {
        return domainMapper.toProtocolParams(protocolParamUpdate);
    }

}
