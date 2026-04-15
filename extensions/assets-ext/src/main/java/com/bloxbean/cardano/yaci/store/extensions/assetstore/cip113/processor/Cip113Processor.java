package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser.Cip113RegistryNodeParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service.Cip113RegistryService;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class Cip113Processor {

    private final Cip113Configuration cip113Configuration;
    private final Cip113RegistryNodeParser registryNodeParser;
    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final Cip113RegistryService cip113RegistryService;

    @EventListener
    @Transactional
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        if (!cip113Configuration.isEnabled()) {
            return;
        }

        Long slot = addressUtxoEvent.getMetadata().getSlot();

        List<Cip113RegistryNode> entities = addressUtxoEvent.getTxInputOutputs()
                .stream()
                .flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .filter(utxo -> utxo.getInlineDatum() != null && cip113RegistryService.containsRegistryNode(utxo))
                .map(utxo -> toEntity(utxo, slot))
                .flatMap(Optional::stream)
                .toList();

        if (!entities.isEmpty()) {
            cip113RegistryNodeRepository.saveAll(entities);
            entities.forEach(e -> log.info("Indexed CIP-113 registry node: key={}, slot={}, txHash={}",
                    e.getKey(), e.getSlot(), e.getTxHash()));
        }
    }

    private Optional<Cip113RegistryNode> toEntity(AddressUtxo utxo, Long slot) {
        return registryNodeParser.parse(utxo.getInlineDatum())
                .map(parsed -> Cip113RegistryNode.builder()
                        .key(parsed.key())
                        .slot(slot)
                        .txHash(utxo.getTxHash())
                        .transferLogicScript(parsed.transferLogicScript())
                        .thirdPartyTransferLogicScript(parsed.thirdPartyTransferLogicScript())
                        .globalStatePolicyId(parsed.globalStatePolicyId())
                        .next(parsed.next())
                        .datum(utxo.getInlineDatum())
                        .lastSyncedAt(LocalDateTime.now())
                        .build());
    }

}
