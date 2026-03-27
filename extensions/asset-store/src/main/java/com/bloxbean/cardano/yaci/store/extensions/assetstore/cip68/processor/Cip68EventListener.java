package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.entity.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.repository.MetadataReferenceNftRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class Cip68EventListener {

    private record ReferenceNftUtxoData(AssetType referenceNft,
                                        Cip68TokenMetadata cip68TokenMetadata,
                                        String datum) {

    }

    private final Cip68TokenService cip68TokenService;
    private final Cip68DatumParser cip68DatumParser;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    @EventListener
    @Transactional
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        Long slot = addressUtxoEvent.getMetadata().getSlot();
        addressUtxoEvent.getTxInputOutputs()
                .stream()
                .flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .flatMap(this::findReferenceNft)
                .flatMap(this::parseDatum)
                .filter(this::isValidMetadata)
                .forEach(referenceNftUtxoData -> {
                    AssetType assetType = referenceNftUtxoData.referenceNft();
                    Cip68TokenMetadata metadata = referenceNftUtxoData.cip68TokenMetadata();
                    MetadataReferenceNft referenceNftEntity = buildMetadataReferenceNft(metadata, assetType, referenceNftUtxoData.datum(), slot);
                    metadataReferenceNftRepository.save(referenceNftEntity);
                });
    }

    private MetadataReferenceNft buildMetadataReferenceNft(Cip68TokenMetadata metadata, AssetType assetType, String datum, Long slot) {
        return MetadataReferenceNft.builder()
                .policyId(assetType.policyId())
                .assetName(assetType.assetName())
                .slot(slot)
                .name(metadata.name())
                .description(metadata.description())
                .ticker(metadata.ticker())
                .url(metadata.url())
                .decimals(metadata.decimals())
                .logo(metadata.logo())
                .version(metadata.version())
                .datum(datum)
                .build();
    }

    private boolean isValidMetadata(ReferenceNftUtxoData referenceNftUtxoData) {
        return cip68TokenService.isValidMetadata(referenceNftUtxoData.cip68TokenMetadata());
    }

    private record AmtWithUtxo(Amt amt, AddressUtxo utxo) {

    }

    private Stream<ReferenceNftUtxoData> parseDatum(AmtWithUtxo referenceNftUtxo) {
        return cip68DatumParser.parse(referenceNftUtxo.utxo().getInlineDatum())
                .stream()
                .map(cip68TokenMetadata -> new ReferenceNftUtxoData(AssetType.fromUnit(referenceNftUtxo.amt().getUnit()),
                        cip68TokenMetadata,
                        referenceNftUtxo.utxo().getInlineDatum()));
    }

    private Stream<AmtWithUtxo> findReferenceNft(AddressUtxo utxo) {
        return cip68TokenService.extractReferenceNft(utxo).map(amt -> new AmtWithUtxo(amt, utxo)).stream();
    }

}
