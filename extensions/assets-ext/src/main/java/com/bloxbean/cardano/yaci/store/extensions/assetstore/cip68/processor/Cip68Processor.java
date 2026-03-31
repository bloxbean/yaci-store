package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;

import java.time.LocalDateTime;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.MetadataReferenceNftRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class Cip68Processor {

    private record ReferenceNftUtxoData(AssetType referenceNft,
                                        FungibleTokenMetadata fungibleTokenMetadata,
                                        String datum) {

    }

    private final Cip68TokenService cip68TokenService;
    private final Cip68DatumParser cip68DatumParser;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    @EventListener
    @Transactional
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        Long slot = addressUtxoEvent.getMetadata().getSlot();
        List<MetadataReferenceNft> entities = addressUtxoEvent.getTxInputOutputs()
                .stream()
                .flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .flatMap(this::findReferenceNft)
                .flatMap(this::parseDatum)
                .filter(this::isValidMetadata)
                .map(referenceNftUtxoData -> buildMetadataReferenceNft(
                        referenceNftUtxoData.fungibleTokenMetadata(),
                        referenceNftUtxoData.referenceNft(),
                        referenceNftUtxoData.datum(), slot))
                .toList();

        if (!entities.isEmpty()) {
            metadataReferenceNftRepository.saveAll(entities);
        }
    }

    private MetadataReferenceNft buildMetadataReferenceNft(FungibleTokenMetadata metadata, AssetType assetType, String datum, Long slot) {
        return MetadataReferenceNft.builder()
                .policyId(assetType.policyId())
                .assetName(assetType.assetName())
                .slot(slot)
                .label(Cip68Constants.labelFromReferenceNft(assetType.assetName()))
                .name(metadata.name())
                .description(metadata.description())
                .ticker(metadata.ticker())
                .url(metadata.url())
                .decimals(metadata.decimals())
                .logo(metadata.logo())
                .version(metadata.version())
                .datum(datum)
                .lastSyncedAt(LocalDateTime.now())
                .build();
    }

    private boolean isValidMetadata(ReferenceNftUtxoData referenceNftUtxoData) {
        return cip68TokenService.isValidMetadata(referenceNftUtxoData.fungibleTokenMetadata());
    }

    private record AmtWithUtxo(Amt amt, AddressUtxo utxo) {

    }

    private Stream<ReferenceNftUtxoData> parseDatum(AmtWithUtxo referenceNftUtxo) {
        return cip68DatumParser.parse(referenceNftUtxo.utxo().getInlineDatum())
                .stream()
                .map(fungibleTokenMetadata -> new ReferenceNftUtxoData(AssetType.fromUnit(referenceNftUtxo.amt().getUnit()),
                        fungibleTokenMetadata,
                        referenceNftUtxo.utxo().getInlineDatum()));
    }

    private Stream<AmtWithUtxo> findReferenceNft(AddressUtxo utxo) {
        return cip68TokenService.extractReferenceNft(utxo).map(amt -> new AmtWithUtxo(amt, utxo)).stream();
    }

}
