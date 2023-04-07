package com.bloxbean.cardano.yaci.store.remote.publisher;

import com.bloxbean.cardano.yaci.store.events.*;
import com.bloxbean.cardano.yaci.store.remote.common.RemoteBindingConstant;
import com.bloxbean.cardano.yaci.store.remote.common.RemoteConfigProperties;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RemoteEventPublisher {
    private final StreamBridge streamBridge;

    public RemoteEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
        log.info("RemoteEventPublisher initialized >>");
    }

    private void publish(String topic, Object event) {
        streamBridge.send(topic + "-out-0", event);
    }

    @EventListener(condition = "#event.getMetadata().isRemotePublish() == false")
    public void handleBlockHeaderEvent(@NonNull BlockHeaderEvent event) {
        if (log.isDebugEnabled())
            log.debug("Publishing event to Kafka: "
                    + event.getBlockHeader().getHeaderBody().getBlockNumber());

        EventMetadata eventMetadata = event.getMetadata().toBuilder()
                .remotePublish(true)
                .build();
        BlockHeaderEvent remoteEvent = new BlockHeaderEvent(eventMetadata, event.getBlockHeader());
        publish(RemoteBindingConstant.blockHeaderEvent, remoteEvent);
    }

    @EventListener(condition = "#event.getMetadata().isRemotePublish() == false")
    public void handleByronEbBlockEvent(@NonNull ByronEbBlockEvent event) {
        if (log.isDebugEnabled())
            log.debug("Publishing event to Kafka: "
                    + event.getEventMetadata().getBlock());

        EventMetadata eventMetadata = event.getEventMetadata().toBuilder()
                .remotePublish(true)
                .build();
        ByronEbBlockEvent remoteEvent = new ByronEbBlockEvent(eventMetadata, event.getByronEbBlock());
        publish(RemoteBindingConstant.byronEbBlockEvent, remoteEvent);
    }

    @EventListener(condition = "#event.getMetadata().isRemotePublish() == false")
    public void handleTransactionEvent(@NonNull TransactionEvent event) {
        if (log.isDebugEnabled())
            log.debug("Publishing event to Kafka: "
                    + event.getMetadata().getBlock());

        EventMetadata eventMetadata = event.getMetadata().toBuilder()
                .remotePublish(true)
                .build();
        TransactionEvent remoteEvent = new TransactionEvent(eventMetadata, event.getTransactions());
        publish(RemoteBindingConstant.transactionEvent, remoteEvent);
    }

    @EventListener(condition = "#event.isRemotePublish() == false")
    public void handleRollbackEvent(@NonNull RollbackEvent event) {
        if (log.isDebugEnabled())
            log.debug("Publishing event to Kafka: "
                    + event);

        RollbackEvent remoteEvent = event.toBuilder()
                .remotePublish(true)
                .build();
        publish(RemoteBindingConstant.rollbackEvent, remoteEvent);
    }

    @EventListener(condition = "#event.getMetadata().isRemotePublish() == false")
    public void handleAuxDataEvent(@NonNull AuxDataEvent event) {
        if (log.isDebugEnabled())
            log.debug("Publishing event to Kafka: "
                    + event);

        EventMetadata metadata = event.getMetadata().toBuilder()
                .remotePublish(true)
                .build();
        AuxDataEvent remoteEvent = new AuxDataEvent(metadata, event.getTxAuxDataList());
        publish(RemoteBindingConstant.auxDataEvent, remoteEvent);
    }

    @EventListener(condition = "#event.getMetadata().isRemotePublish() == false")
    public void handleCertificationEvent(@NonNull CertificateEvent event) {
        if (log.isDebugEnabled())
            log.debug("Publishing event to Kafka: "
                    + event.getMetadata().getBlock());

        EventMetadata metadata = event.getMetadata().toBuilder()
                .remotePublish(true)
                .build();
        CertificateEvent remoteEvent = new CertificateEvent(metadata, event.getTxCertificatesList());
        publish(RemoteBindingConstant.certificateEvent, remoteEvent);
    }

    @EventListener(condition = "#event.isRemotePublish() == false")
    public void handleGenesisBlockEvent(@NonNull GenesisBlockEvent event) {
        if (log.isDebugEnabled())
            log.debug("Publishing event to Kafka: "
                    + event);

        GenesisBlockEvent remoteEvent = event.toBuilder()
                .remotePublish(true)
                .build();
        publish(RemoteBindingConstant.genesisBlockEvent, remoteEvent);
    }

    @EventListener(condition = "#event.getMetadata().isRemotePublish() == false")
    public void handleMintBurnEvent(@NonNull MintBurnEvent event) {
        if (log.isDebugEnabled())
            log.debug("Publishing event to Kafka: "
                    + event);

        EventMetadata metadata = event.getMetadata().toBuilder()
                .remotePublish(true)
                .build();
        MintBurnEvent remoteEvent = new MintBurnEvent(metadata, event.getTxMintBurns());
        publish(RemoteBindingConstant.mintBurnEvent, remoteEvent);
    }

    @EventListener(condition = "#event.getMetadata().isRemotePublish() == false")
    public void handleScriptEvent(@NonNull ScriptEvent event) {
        if (log.isDebugEnabled())
            log.debug("Publishing event to Kafka: "
                    + event);

        EventMetadata metadata = event.getMetadata().toBuilder()
                .remotePublish(true)
                .build();
        ScriptEvent remoteEvent = new ScriptEvent(metadata, event.getTxScriptsList());
        publish(RemoteBindingConstant.scriptEvent, remoteEvent);
    }
}
