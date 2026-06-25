package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "store.assets.ext.cip68.enabled", havingValue = "true", matchIfMissing = true)
public class Cip68Processor {

    private final Cip68TokenService cip68TokenService;
    private final Cip68DatumParser cip68DatumParser;
    private final Cip68MetadataRepository cip68MetadataRepository;

    @EventListener
    @Transactional
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        Long slot = addressUtxoEvent.getMetadata().getSlot();

        // Iterate per-transaction so we keep the transaction's full output set in context.
        // Label classification needs to look at the OTHER outputs in the same tx to find
        // the co-minted user token (000de140 / 0014df10 / 001bc280); a flat-map across
        // all txs would lose that boundary.
        List<Cip68Metadata> entities = new ArrayList<>();
        for (TxInputOutput txIo : addressUtxoEvent.getTxInputOutputs()) {
            Set<String> coMintedPrefixesInTx = collectCoMintedPrefixes(txIo);

            for (AddressUtxo output : txIo.getOutputs()) {
                cip68TokenService.extractReferenceNft(output).ifPresent(refNftAmt -> {
                    cip68DatumParser.parse(output.getInlineDatum()).ifPresent(parsed -> {
                        if (!cip68TokenService.isValidMetadata(parsed)) {
                            return;
                        }
                        AssetType refNftAssetType = AssetType.fromUnit(refNftAmt.getUnit());
                        int label = deriveLabel(refNftAssetType, coMintedPrefixesInTx);
                        entities.add(buildCip68Metadata(
                                parsed, refNftAssetType, output.getInlineDatum(), slot,
                                output.getTxHash(), output.getTxIndex(), label));
                    });
                });
            }
        }

        if (!entities.isEmpty()) {
            cip68MetadataRepository.saveAll(entities);
        }
    }

    /**
     * Determine the CIP-68 user-token label by looking at the co-minted user token
     * present in the same transaction. CIP-68 always co-mints the reference NFT (label 100)
     * with one of the user-token prefixes — this method picks which one.
     * <p>
     * If no co-minted user token is found at the same policy ID with a recognised CIP-68
     * prefix, falls back to {@link Cip68Constants#LABEL_FT}. This matches the prior
     * default-to-FT behaviour for orphan reference NFTs and odd one-off mints that don't
     * follow the standard pattern.
     */
    private int deriveLabel(AssetType refNftAssetType, Set<String> coMintedPrefixes) {
        if (coMintedPrefixes.contains(Cip68Constants.NFT_TOKEN_PREFIX)) {
            return Cip68Constants.LABEL_NFT;
        }
        if (coMintedPrefixes.contains(Cip68Constants.FUNGIBLE_TOKEN_PREFIX)) {
            return Cip68Constants.LABEL_FT;
        }
        if (coMintedPrefixes.contains(Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX)) {
            return Cip68Constants.LABEL_RFT;
        }
        // Orphan reference NFT (no co-minted user token in this tx) or non-standard mint.
        // Default to FT — the historical behaviour and the most common case in practice.
        log.debug("No CIP-68 user-token prefix co-minted with reference NFT {}/{}; defaulting label to FT (333)",
                refNftAssetType.policyId(), refNftAssetType.assetName());
        return Cip68Constants.LABEL_FT;
    }

    /**
     * Walk the transaction's full output set and collect every CIP-68 user-token prefix
     * (000de140 / 0014df10 / 001bc280) appearing at any policy ID. We only need the
     * SET of prefixes, not which policy they belong to, because in practice CIP-68
     * mints are 1:1 — one reference NFT + one user token at the same base name. The
     * presence of an NFT-prefix token anywhere in the tx is a reliable signal that
     * the ref NFT was minted alongside an NFT, not a FT.
     */
    private Set<String> collectCoMintedPrefixes(TxInputOutput txIo) {
        return txIo.getOutputs().stream()
                .flatMap(o -> o.getAmounts().stream())
                .map(this::extractCip68UserTokenPrefix)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** Returns the 4-byte CIP-68 user-token prefix if the asset matches one, else null. */
    private String extractCip68UserTokenPrefix(Amt amt) {
        AssetType at = AssetType.fromUnit(amt.getUnit());
        String assetName = at.assetName();
        if (assetName == null || assetName.length() < 8) {
            return null;
        }
        String prefix = assetName.substring(0, 8);
        if (Cip68Constants.NFT_TOKEN_PREFIX.equals(prefix)
                || Cip68Constants.FUNGIBLE_TOKEN_PREFIX.equals(prefix)
                || Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX.equals(prefix)) {
            return prefix;
        }
        return null;
    }

    private Cip68Metadata buildCip68Metadata(ParsedCip68Datum parsed,
                                             AssetType assetType,
                                             String datum,
                                             Long slot,
                                             String txHash,
                                             Integer txIndex,
                                             int label) {
        return Cip68Metadata.builder()
                .policyId(assetType.policyId())
                .assetName(assetType.assetName())
                .slot(slot)
                .txHash(txHash)
                .txIndex(txIndex)
                .label(label)
                .name(parsed.name())
                .description(parsed.description())
                .ticker(parsed.ticker())
                .url(parsed.url())
                .decimals(parsed.decimals())
                .logo(parsed.logo())
                .image(parsed.image())
                .mediaType(parsed.mediaType())
                .version(parsed.version())
                .datum(datum)
                .properties(parsed.properties())
                .lastSyncedAt(LocalDateTime.now())
                .build();
    }
}
