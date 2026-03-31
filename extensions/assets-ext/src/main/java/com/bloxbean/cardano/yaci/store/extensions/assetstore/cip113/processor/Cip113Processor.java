package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.entity.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser.Cip113RegistryNodeParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.repository.Cip113RegistryNodeRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service.Cip113RegistryService;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

        addressUtxoEvent.getTxInputOutputs()
                .stream()
                .flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .filter(utxo -> utxo.getInlineDatum() != null && cip113RegistryService.containsRegistryNode(utxo))
                .forEach(output -> parseAndSave(output, slot));
    }

    private void parseAndSave(AddressUtxo utxo, Long slot) {
        registryNodeParser.parse(utxo.getInlineDatum())
                .ifPresentOrElse(
                        parsed -> {
                            Cip113RegistryNode entity = Cip113RegistryNode.builder()
                                    .policyId(parsed.key())
                                    .slot(slot)
                                    .txHash(utxo.getTxHash())
                                    .transferLogicScript(parsed.transferLogicScript())
                                    .thirdPartyTransferLogicScript(parsed.thirdPartyTransferLogicScript())
                                    .globalStatePolicyId(parsed.globalStatePolicyId())
                                    .nextKey(parsed.next())
                                    .datum(utxo.getInlineDatum())
                                    .lastSyncedAt(java.time.LocalDateTime.now())
                                    .build();

                            cip113RegistryNodeRepository.save(entity);
                            log.info("Indexed CIP-113 registry node: policyId={}, slot={}, txHash={}",
                                    parsed.key(), slot, utxo.getTxHash());
                        },
                        () -> log.warn("Failed to parse CIP-113 registry node datum from txHash={}",
                                utxo.getTxHash())
                );
    }

}
