package com.bloxbean.cardano.yaci.store.template.processor;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.MintBurnEvent;
import com.bloxbean.cardano.yaci.store.template.domain.MintType;
import com.bloxbean.cardano.yaci.store.template.model.TxAssetEntity;
import com.bloxbean.cardano.yaci.store.template.repository.TxAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssetMintBurnProcessor {
    private final TxAssetRepository txAssetRepository;

    @EventListener
    @Transactional
    public void handleAssetMintBurn(MintBurnEvent mintBurnEvent) {
        EventMetadata eventMetadata = mintBurnEvent.getMetadata();

        List<TxAssetEntity> txAssetList = mintBurnEvent.getTxMintBurns().stream()
                .filter(txMintBurn -> txMintBurn.getAmounts() != null)
                .flatMap(txMintBurn ->
                    txMintBurn.getAmounts()
                            .stream().map(amount -> TxAssetEntity.builder()
                                    .slot(eventMetadata.getSlot())
                                    .txHash(txMintBurn.getTxHash())
                                    .policy(amount.getPolicyId())
                                    .assetName(amount.getAssetName())
                                    .unit(amount.getUnit())
                                    .quantity(amount.getQuantity())
                                    .mintType(amount.getQuantity().compareTo(BigInteger.ZERO) == 1? MintType.MINT : MintType.BURN)
                                    .build()))
                .collect(Collectors.toList());

        if (txAssetList.size() > 0) {
            if (log.isDebugEnabled())
                log.debug("Save assets : " + txAssetList.size());
            txAssetRepository.saveAll(txAssetList);
        }
    }
}
