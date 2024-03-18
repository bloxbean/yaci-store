package com.bloxbean.cardano.yaci.store.epoch.service;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamQueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamsQuery;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.epoch.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.LocalProtocolParamsEntityJpa;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.LocalProtocolParamsRepository;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
@Slf4j
public class LocalProtocolParamService {
    private final LocalClientProvider localClientProvider;
    private final LocalStateQueryClient localStateQueryClient;
    private LocalProtocolParamsRepository protocolParamsRepository;

    private DomainMapper domainMapper = DomainMapper.INSTANCE;

    @Value("${store.cardano.n2c-era:Babbage}")
    private String eraStr;

    private Era era;

    public LocalProtocolParamService(LocalClientProvider localClientProvider, LocalProtocolParamsRepository protocolParamsRepository) {
        this.localClientProvider = localClientProvider;
        this.localStateQueryClient = localClientProvider.getLocalStateQueryClient();
        this.protocolParamsRepository = protocolParamsRepository;
        log.info("ProtocolParamService initialized >>>");
    }

    @PostConstruct
    public void postConstruct() {
        if (StringUtil.isEmpty(eraStr))
            eraStr = "Babbage";

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
        }
    }

    @Transactional
    public void fetchAndSetCurrentProtocolParams() {
        getCurrentProtocolParamsFromNode()
                .doOnError(throwable -> {
                    log.error("Local protocol param sync error {}", throwable.getMessage());
                })
                .subscribe(protocolParamUpdate -> {
                    LocalProtocolParamsEntityJpa entity = new LocalProtocolParamsEntityJpa();
                    entity.setId(1L);
                    entity.setProtocolParams(convertProtoParams(protocolParamUpdate));

                    protocolParamsRepository.save(entity);
                });
    }

    public Optional<ProtocolParams> getCurrentProtocolParams() {
        return protocolParamsRepository.findById(Long.valueOf(1))
                .map(protocolParamsEntity -> Optional.ofNullable(protocolParamsEntity.getProtocolParams()))
                .orElse(Optional.empty());
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
