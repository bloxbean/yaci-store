package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.entity.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68FTDatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.repository.MetadataReferenceNftRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68FungibleTokenService;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class Cip68EventListener {

    private record ReferenceNftUtxoData(AssetType referenceNft,
                                        FungibleTokenMetadata fungibleTokenMetadata,
                                        String datum) {

    }

    private final Cip68FungibleTokenService cip68FungibleTokenService;
    private final Cip68FTDatumParser cip68DatumParser;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    @EventListener
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        Long slot = addressUtxoEvent.getMetadata().getSlot();
        addressUtxoEvent.getTxInputOutputs()
                .stream()
                .flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .flatMap(this::findReferenceNft)
                .flatMap(this::parseDatum)
                .filter(this::isValidFTMetadata)
                .forEach(referenceNftUtxoData -> {
                    AssetType assetType = referenceNftUtxoData.referenceNft();
                    FungibleTokenMetadata metadata = referenceNftUtxoData.fungibleTokenMetadata();
                    MetadataReferenceNft referenceNftEntity = buildMetadataReferenceNft(metadata, assetType, referenceNftUtxoData.datum(), slot);
                    metadataReferenceNftRepository.save(referenceNftEntity);
                });
    }

    private MetadataReferenceNft buildMetadataReferenceNft(FungibleTokenMetadata metadata, AssetType assetType, String datum, Long slot) {
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

    private boolean isValidFTMetadata(ReferenceNftUtxoData referenceNftUtxoData) {
        return cip68FungibleTokenService.isValidFTMetadata(referenceNftUtxoData.fungibleTokenMetadata());
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
        return cip68FungibleTokenService.extractReferenceNft(utxo).map(amt -> new AmtWithUtxo(amt, utxo)).stream();
    }

}
