package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.DatumRejection;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@ConditionalOnProperty(name = "store.assets.ext.cip68.enabled", havingValue = "true", matchIfMissing = true)
public class Cip68Processor {

    private final Cip68TokenService cip68TokenService;
    private final Cip68DatumParser cip68DatumParser;
    private final Cip68MetadataRepository cip68MetadataRepository;
    private final Cip68Metrics metrics;

    @Autowired
    public Cip68Processor(Cip68TokenService cip68TokenService, Cip68DatumParser cip68DatumParser,
                          Cip68MetadataRepository cip68MetadataRepository, Cip68Metrics metrics) {
        this.cip68TokenService = cip68TokenService;
        this.cip68DatumParser = cip68DatumParser;
        this.cip68MetadataRepository = cip68MetadataRepository;
        this.metrics = metrics;
    }

    /** Without metrics that anyone reads (tests). */
    public Cip68Processor(Cip68TokenService cip68TokenService, Cip68DatumParser cip68DatumParser,
                          Cip68MetadataRepository cip68MetadataRepository) {
        this(cip68TokenService, cip68DatumParser, cip68MetadataRepository, Cip68Metrics.noop());
    }

    @EventListener
    @Transactional
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        Long slot = addressUtxoEvent.getMetadata().getSlot();

        // Iterate per-transaction so we keep the transaction's full output set in context.
        // Label classification needs to look at the OTHER outputs in the same tx to find
        // the co-minted user token (000de140 / 0014df10 / 001bc280) paired with the reference
        // NFT; a flat-map across all txs would lose that boundary.
        List<Cip68Metadata> entities = new ArrayList<>();
        for (TxInputOutput txIo : addressUtxoEvent.getTxInputOutputs()) {
            Set<String> assetUnitsInTx = collectAssetUnits(txIo);

            for (AddressUtxo output : txIo.getOutputs()) {
                for (Amt refNftAmt : referenceNftsToIndex(output)) {
                    toEntity(output, AssetType.fromUnit(refNftAmt.getUnit()), assetUnitsInTx, slot).ifPresent(entities::add);
                }
            }
        }

        if (!entities.isEmpty()) {
            cip68MetadataRepository.saveAll(entities);
        }
    }

    /** The row for one reference NFT of an output, or empty if its datum cannot be parsed or is not valid for its labels. */
    private Optional<Cip68Metadata> toEntity(AddressUtxo output, AssetType refNft, Set<String> assetUnitsInTx, Long slot) {
        return cip68DatumParser.parse(output.getInlineDatum(), refNft).flatMap(parsed -> {
            // The labels come first: which fields a datum must have depends on them. A reference NFT can have user
            // tokens of several labels (the CIP allows it), and then the datum has to satisfy the requirements of
            // every one of them.
            List<Integer> labels = deriveLabels(refNft, assetUnitsInTx, parsed);
            if (!satisfiesEveryLabel(parsed, refNft, labels)) {
                return Optional.empty();
            }
            ParsedCip68Datum stored = withoutInvalidLogo(parsed, refNft);
            int label = labels.getFirst();
            metrics.datumIndexed(label);
            if (labels.size() > 1) {
                metrics.multiLabel(labels, Cip68Metrics.INDEXED_OUTCOME);
                log.info("CIP-68 reference NFT {}/{} has user tokens for labels {}; the datum satisfies all of them, "
                                + "stored with label {}",
                        refNft.policyId(), refNft.assetName(), labels, label);
            }
            return Optional.of(buildCip68Metadata(stored, refNft, output.getInlineDatum(), slot,
                    output.getTxHash(), output.getTxIndex(), label));
        });
    }

    /**
     * The logo is optional, so a bad one costs only the logo: it is left out with a warning and the rest of the datum
     * is indexed.
     */
    private ParsedCip68Datum withoutInvalidLogo(ParsedCip68Datum parsed, AssetType refNft) {
        Optional<String> reason = cip68TokenService.invalidLogoReason(parsed);
        if (reason.isEmpty()) {
            return parsed;
        }
        log.warn("CIP-68 datum of {}/{}: dropping the logo and keeping the rest, because {}",
                refNft.policyId(), refNft.assetName(), reason.get());
        metrics.propertyDropped(Cip68Metrics.BAD_LOGO_SCHEME);
        return parsed.withoutLogo();
    }

    /**
     * Checks the datum against the requirements of each label in turn. The first label it fails gives the warning
     * and the metric, and the datum is not indexed.
     */
    private boolean satisfiesEveryLabel(ParsedCip68Datum parsed, AssetType refNft, List<Integer> labels) {
        for (int candidate : labels) {
            Optional<DatumRejection> rejection = cip68TokenService.rejection(parsed, candidate);
            if (rejection.isPresent()) {
                warnSkipped(refNft, labels, candidate, rejection.get().message());
                metrics.datumSkipped(candidate, rejection.get().reason());
                if (labels.size() > 1) {
                    metrics.multiLabel(labels, Cip68Metrics.SKIPPED_OUTCOME);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Determine the CIP-68 user-token labels from the user tokens paired with the reference NFT.
     * <p>
     * CIP-68 pairs the two by policy and base name: the reference NFT is {@code 000643b0 + base},
     * its user token is a label prefix ({@code 000de140} / {@code 0014df10} / {@code 001bc280}) plus
     * the same base, under the same policy. Only that pair counts. A user token of another policy,
     * or with another base name, that merely sits in the same transaction says nothing about this
     * reference NFT: a transaction can mint several unrelated CIP-68 tokens.
     * <p>
     * The CIP allows several user tokens for one reference NFT, so a transaction can pair it with a 222 and a 333
     * (or 444) token. Every paired label is returned, in the order 222, 333, 444: the first is the label the row
     * is stored with, and the datum has to satisfy the requirements of all of them. If none is paired (an orphan
     * reference NFT, or its user token was minted in another transaction), the label is inferred from the shape of
     * the datum, see {@link #inferLabelFromDatum}, and it is the only one.
     */
    private List<Integer> deriveLabels(AssetType refNftAssetType, Set<String> assetUnitsInTx, ParsedCip68Datum parsed) {
        String baseName = refNftAssetType.assetName().substring(Cip68Constants.REFERENCE_TOKEN_PREFIX.length());

        List<Integer> labels = new ArrayList<>();
        if (hasPairedUserToken(assetUnitsInTx, refNftAssetType, Cip68Constants.NFT_TOKEN_PREFIX, baseName)) {
            labels.add(Cip68Constants.LABEL_NFT);
        }
        if (hasPairedUserToken(assetUnitsInTx, refNftAssetType, Cip68Constants.FUNGIBLE_TOKEN_PREFIX, baseName)) {
            labels.add(Cip68Constants.LABEL_FT);
        }
        if (hasPairedUserToken(assetUnitsInTx, refNftAssetType, Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX, baseName)) {
            labels.add(Cip68Constants.LABEL_RFT);
        }
        if (!labels.isEmpty()) {
            return labels;
        }
        int inferred = inferLabelFromDatum(parsed);
        log.debug("No CIP-68 user token paired with reference NFT {}/{} in this tx; label {} inferred from the datum",
                refNftAssetType.policyId(), refNftAssetType.assetName(), inferred);
        return List.of(inferred);
    }

    /**
     * Label of a reference NFT whose user token is not in the same transaction, guessed from which fields
     * the datum carries. It is a heuristic, used only when the exact pairing is not available.
     * <ul>
     *   <li>{@code ticker} or {@code logo}, which only the fungible token defines: 333.</li>
     *   <li>{@code image}, {@code mediaType} or {@code files}, the NFT and RFT fields: 444 when
     *       {@code decimals} is also present (the RFT has it, the NFT does not), otherwise 222.</li>
     *   <li>Neither: 333, the historical default.</li>
     * </ul>
     * {@code decimals} alone does not decide, since both the fungible token and the RFT define it.
     */
    private static int inferLabelFromDatum(ParsedCip68Datum parsed) {
        if (parsed.ticker() != null || parsed.logo() != null) {
            return Cip68Constants.LABEL_FT;
        }
        boolean hasFiles = parsed.properties() != null && parsed.properties().containsKey("files");
        if (parsed.image() != null || parsed.mediaType() != null || hasFiles) {
            return parsed.decimals() != null ? Cip68Constants.LABEL_RFT : Cip68Constants.LABEL_NFT;
        }
        return Cip68Constants.LABEL_FT;
    }

    private static boolean hasPairedUserToken(Set<String> assetUnitsInTx, AssetType refNftAssetType,
                                              String userTokenPrefix, String baseName) {
        return assetUnitsInTx.contains(normalize(refNftAssetType.policyId() + userTokenPrefix + baseName));
    }

    /** Every asset unit (policy id + asset name) in the transaction's outputs, normalised for comparison. */
    private Set<String> collectAssetUnits(TxInputOutput txIo) {
        return txIo.getOutputs().stream()
                .flatMap(o -> o.getAmounts().stream())
                .map(amt -> normalize(AssetType.fromUnit(amt.getUnit()).toUnit()))
                .collect(Collectors.toSet());
    }

    private static String normalize(String unit) {
        return unit.toLowerCase(Locale.ROOT);
    }

    /**
     * The reference NFTs of an output whose datum should be indexed: all of them. An output with one reference NFT
     * is the normal case. With several, a nested (version 4) datum carries the metadata of each, each resolved to its
     * own entry. A flat datum has no entry per token, but the CIP's retrieval steps look up the output of the
     * reference NFT and read its datum, whatever else the output holds, so a flat datum is the metadata of every
     * reference NFT in the output. They are all indexed with it, and one warning lists them so the case can be audited.
     */
    private List<Amt> referenceNftsToIndex(AddressUtxo output) {
        List<Amt> refNfts = cip68TokenService.extractReferenceNfts(output);
        if (refNfts.size() > 1 && !cip68DatumParser.hasNestedMetadata(output.getInlineDatum())) {
            log.warn("Output {}#{} holds {} reference NFTs with a flat CIP-68 datum; CIP-68 gives a flat datum to every "
                            + "reference NFT in the output, so indexing it for all of them: {}",
                    output.getTxHash(), output.getOutputIndex(), refNfts.size(),
                    refNfts.stream().map(Amt::getUnit).collect(Collectors.joining(", ")));
        }
        return refNfts;
    }

    /**
     * A datum that is not indexed because it breaks what CIP-68 requires leaves a trace, not a silent gap. With
     * several labels, the warning also says which of them the datum fails.
     */
    private void warnSkipped(AssetType refNftAssetType, List<Integer> labels, int failedLabel, String reason) {
        if (labels.size() == 1) {
            log.warn("Skipping CIP-68 datum of {}/{} (label {}): {}",
                    refNftAssetType.policyId(), refNftAssetType.assetName(), failedLabel, reason);
        } else {
            log.warn("Skipping CIP-68 datum of {}/{} (labels {}): it is not valid as label {}, because {}",
                    refNftAssetType.policyId(), refNftAssetType.assetName(), labels, failedLabel, reason);
        }
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
