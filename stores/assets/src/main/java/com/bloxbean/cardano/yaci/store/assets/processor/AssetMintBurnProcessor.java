package com.bloxbean.cardano.yaci.store.assets.processor;

import com.bloxbean.cardano.client.api.util.AssetUtil;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.yaci.store.assets.domain.MintType;
import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.domain.TxAssetEvent;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.MintBurnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final AssetStorage assetStorage;
    private final ApplicationEventPublisher publisher;

    @EventListener
    @Transactional
    public void handleAssetMintBurn(MintBurnEvent mintBurnEvent) {
        EventMetadata eventMetadata = mintBurnEvent.getMetadata();

        List<TxAsset> txAssetList = mintBurnEvent.getTxMintBurns().stream()
                .filter(txMintBurn -> txMintBurn.getAmounts() != null)
                .flatMap(txMintBurn ->
                    txMintBurn.getAmounts()
                            .stream().map(amount -> TxAsset.builder()
                                    .slot(eventMetadata.getSlot())
                                    .txHash(txMintBurn.getTxHash())
                                    .policy(amount.getPolicyId())
                                    .assetName(amount.getAssetName())
                                    .unit(amount.getUnit())
                                    .fingerprint(getFingerprint(amount.getPolicyId(), amount.getAssetName()))
                                    .quantity(amount.getQuantity())
                                    .mintType(amount.getQuantity().compareTo(BigInteger.ZERO) == 1? MintType.MINT : MintType.BURN)
                                    .blockNumber(eventMetadata.getBlock())
                                    .blockTime(eventMetadata.getBlockTime())
                                    .build()))
                .collect(Collectors.toList());

        if (txAssetList.size() > 0) {
            if (log.isDebugEnabled())
                log.debug("Save assets : " + txAssetList.size());
            assetStorage.saveAll(txAssetList);

            //biz events
            publisher.publishEvent(new TxAssetEvent(eventMetadata, txAssetList));
        }
    }

    private String getFingerprint(String policyId, String assetName) {
        try {
            return AssetUtil.calculateFingerPrint(policyId, new Asset(assetName, BigInteger.ZERO).getNameAsHex());
        } catch (Exception e) {
            log.error("Error calculating fingerprint for policy: " + policyId + ", asset: " + assetName, e);
            return null;
        }
    }
}
