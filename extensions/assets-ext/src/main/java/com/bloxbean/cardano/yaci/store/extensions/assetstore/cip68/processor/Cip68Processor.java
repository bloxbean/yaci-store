package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParseOutcome;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenRecorder;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReason;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReport;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68Validator;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@ConditionalOnProperty(name = "store.assets.ext.cip68.enabled", havingValue = "true", matchIfMissing = true)
public class Cip68Processor {

    /**
     * Used only by {@link #requireSerializableProperties}, to write the {@code properties} JSON once, early, so that a
     * value Jackson refuses is found while the output is being built. Hibernate does the same write later with its
     * own default mapper, at flush time inside the block transaction, where an exception stops the whole sync.
     * A plain {@code new ObjectMapper()} has the same default limits (for example a nesting depth of 1000). Do not
     * remove it as "unused serialization": see https://github.com/bloxbean/yaci-store/issues/1240 and
     * https://github.com/bloxbean/yaci-store/issues/1243.
     */
    private static final ObjectMapper PROPERTIES_MAPPER = new ObjectMapper();

    private final Cip68TokenService cip68TokenService;
    private final Cip68DatumParser cip68DatumParser;
    private final Cip68MetadataRepository cip68MetadataRepository;
    private final Cip68Metrics metrics;
    private final InvalidTokenRecorder invalidTokenRecorder;

    @Autowired
    public Cip68Processor(Cip68TokenService cip68TokenService, Cip68DatumParser cip68DatumParser,
                          Cip68MetadataRepository cip68MetadataRepository, Cip68Metrics metrics,
                          InvalidTokenRecorder invalidTokenRecorder) {
        this.cip68TokenService = cip68TokenService;
        this.cip68DatumParser = cip68DatumParser;
        this.cip68MetadataRepository = cip68MetadataRepository;
        this.metrics = metrics;
        this.invalidTokenRecorder = invalidTokenRecorder;
    }

    /** Without a table for what is dropped (tests). */
    public Cip68Processor(Cip68TokenService cip68TokenService, Cip68DatumParser cip68DatumParser,
                          Cip68MetadataRepository cip68MetadataRepository, Cip68Metrics metrics) {
        this(cip68TokenService, cip68DatumParser, cip68MetadataRepository, metrics, InvalidTokenRecorder.noop());
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
                try {
                    indexOutput(output, assetUnitsInTx, slot, entities);
                } catch (RuntimeException e) {
                    skipOutput(output, slot, e);
                }
            }
        }

        if (!entities.isEmpty()) {
            cip68MetadataRepository.saveAll(entities);
        }
    }

    private void indexOutput(AddressUtxo output, Set<String> assetUnitsInTx, Long slot, List<Cip68Metadata> entities) {
        List<Amt> refNfts = cip68TokenService.extractReferenceNfts(output);
        SharedDatum shared = null;
        if (refNfts.size() > 1 && !cip68DatumParser.hasNestedMetadata(output.getInlineDatum())) {
            logFlatDatumOfSeveralReferenceNfts(output, refNfts);
            shared = new SharedDatum(output, refNfts.size());
        }
        for (Amt refNftAmt : refNfts) {
            AssetType refNft = AssetType.fromUnit(refNftAmt.getUnit());
            // one reference NFT failing must not take the others of the output with it: only that one is skipped
            try {
                toEntity(output, refNft, assetUnitsInTx, slot, shared).ifPresent(entities::add);
            } catch (RuntimeException e) {
                skipReferenceNft(output, slot, refNft, e);
            }
        }
    }

    /**
     * An unexpected exception while indexing a reference NFT means the datum is something the parser did not foresee, and
     * on a public chain anyone can put a datum there. That reference NFT is skipped, with a warning and a count, and the
     * sync goes on: the metadata of a token is not worth stopping the store for. The other reference NFTs of the output,
     * and the rows already built, are kept. A failure that only shows at flush or commit, inside the database, is not
     * caught here.
     * <p>
     * Why this catch exists: this listener runs inside the block transaction, so any exception stops the whole store
     * (https://github.com/bloxbean/yaci-store/issues/1243, ADR 0004 in {@code extensions/assets-ext/docs/adr}).
     * The metadata of a token is the least critical thing the store holds, so a bad datum is skipped and counted as
     * {@code datums.skipped{reason="processing_failure"}}. That counter is the one to alert on: unlike the other
     * reasons it means something unexpected, a bug or a new kind of datum.
     */
    private void skipReferenceNft(AddressUtxo output, Long slot, AssetType refNft, RuntimeException e) {
        log.warn("Output {}#{}: skipping the CIP-68 datum of {}, indexing it failed unexpectedly ({}: {}); datum {}",
                output.getTxHash(), output.getOutputIndex(), refNft.policyId() + "/" + refNft.assetName(), e.getClass().getSimpleName(), e.getMessage(),
                output.getInlineDatum());
        log.debug("Stack trace of the CIP-68 indexing failure of output {}#{}", output.getTxHash(), output.getOutputIndex(), e);
        metrics.processingFailure();
        recordProcessingFailure(output, slot, refNft, e);
    }

    /**
     * The same for a failure that cannot be put on one reference NFT: reading the output's reference NFTs, or the datum that
     * all of them share, failed before any of them was indexed. The whole output is skipped.
     */
    private void skipOutput(AddressUtxo output, Long slot, RuntimeException e) {
        log.warn("Output {}#{}: skipping the CIP-68 datum, indexing it failed unexpectedly ({}: {}); datum {}",
                output.getTxHash(), output.getOutputIndex(), e.getClass().getSimpleName(), e.getMessage(),
                output.getInlineDatum());
        log.debug("Stack trace of the CIP-68 indexing failure of output {}#{}", output.getTxHash(), output.getOutputIndex(), e);
        metrics.processingFailure();
        List<Amt> refNfts;
        try {
            refNfts = cip68TokenService.extractReferenceNfts(output);
        } catch (RuntimeException ignored) {
            refNfts = List.of();
        }
        if (refNfts.isEmpty()) {
            recordProcessingFailure(output, slot, null, e);
        }
        for (Amt refNft : refNfts) {
            recordProcessingFailure(output, slot, AssetType.fromUnit(refNft.getUnit()), e);
        }
    }

    /** One row of {@code invalid_token} (ADR 0007) for the reference NFT that failed, or for the output when none can be named; a failure here is already contained by the recorder. */
    private void recordProcessingFailure(AddressUtxo output, Long slot, @Nullable AssetType refNft, RuntimeException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("exception", e.getClass().getName());
        details.put("message", String.valueOf(e.getMessage()));
        details.put("datum_prefix", abbreviateDatum(output.getInlineDatum()));
        invalidTokenRecorder.report(droppedReport(output, slot, refNft, InvalidTokenReason.PROCESSING_FAILURE,
                "indexing the output failed unexpectedly: " + e.getClass().getSimpleName() + ": " + e.getMessage(), details));
    }

    /** A datum that is dropped as unusable or an attack is recorded for the reference NFT it was read for (ADR 0007). */
    private void recordDropped(AddressUtxo output, Long slot, AssetType refNft, String reasonCode, String message,
                               @Nullable SharedDatum shared) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("datum_prefix", abbreviateDatum(output.getInlineDatum()));
        details.put("datum_bytes", output.getInlineDatum() == null ? 0 : output.getInlineDatum().length() / 2);
        if (shared != null) {
            details.put("reference_nfts_in_output", shared.referenceNfts);
        }
        invalidTokenRecorder.report(droppedReport(output, slot, refNft, reasonCode, message, details));
    }

    private static InvalidTokenReport droppedReport(AddressUtxo output, Long slot, @Nullable AssetType refNft, String reasonCode,
                                                    String reason, Map<String, Object> details) {
        return InvalidTokenReport.builder()
                .standard(InvalidTokenReason.CIP_68)
                .reasonCode(reasonCode)
                .reason(reason)
                .policyId(refNft == null ? null : refNft.policyId())
                .assetName(refNft == null ? null : refNft.assetName())
                .subject(refNft == null ? null : refNft.toUnit())
                .slot(slot)
                .txHash(output.getTxHash())
                .txIndex(output.getTxIndex())
                .outputIndex(output.getOutputIndex())
                .details(details)
                .build();
    }

    /** A datum can be 16 KB: the table keeps its start, which is enough to find it. */
    private static String abbreviateDatum(@Nullable String datum) {
        if (datum == null) {
            return "";
        }
        return datum.length() <= 256 ? datum : datum.substring(0, 256) + "...";
    }

    /**
     * The row for one reference NFT of an output, or empty if its datum cannot be parsed. A datum is stored whatever
     * is wrong with it: what departs from CIP-68 is kept in the row as {@code validation_errors}, and the API returns
     * it (ADR 0006). {@code shared} is set when the datum is a flat one that the output's several reference NFTs
     * have in common.
     */
    private Optional<Cip68Metadata> toEntity(AddressUtxo output, AssetType refNft, Set<String> assetUnitsInTx, Long slot,
                                             @Nullable SharedDatum shared) {
        ParseOutcome outcome = shared != null
                ? shared.outcome
                : cip68DatumParser.parseDetailed(output.getInlineDatum(), refNft);
        if (outcome instanceof ParseOutcome.Parsed(ParsedCip68Datum parsed)) {
            return Optional.of(indexDatum(parsed, output, refNft, assetUnitsInTx, slot, shared));
        }
        // a datum shared by several reference NFTs is dropped once for the output: recorded once, for the first
        if (outcome instanceof ParseOutcome.Dropped(String reasonCode, String message) && (shared == null || shared.markReported())) {
            recordDropped(output, slot, refNft, reasonCode, message, shared);
        }
        return Optional.empty();
    }

    /** The row for a datum that was read: its labels, the errors it gets for each, and what is reported about it. */
    private Cip68Metadata indexDatum(ParsedCip68Datum parsed, AddressUtxo output, AssetType refNft, Set<String> assetUnitsInTx,
                                     Long slot, @Nullable SharedDatum shared) {
        // The labels come first: which fields a datum must have depends on them. A reference NFT can have user
        // tokens of several labels (the CIP allows it), and then the datum is checked against every one of them.
        List<Integer> labels = deriveLabels(refNft, assetUnitsInTx, parsed);
        // what is wrong with the datum itself does not depend on the label (the parser's errors, and the logo)
        List<ValidationError> datumErrors = new ArrayList<>(parsed.validationErrors());
        ValidationError logoError = Cip68Validator.validateLogo(parsed);
        if (logoError != null) {
            datumErrors.add(logoError);
        }
        List<ValidationError> errors = new ArrayList<>(datumErrors);
        for (int candidate : labels) {
            errors.addAll(Cip68Validator.validate(parsed, candidate));
        }

        int label = labels.getFirst();
        Cip68Metadata entity = buildCip68Metadata(parsed, errors, refNft, output, slot, label);
        requireSerializableProperties(entity);
        metrics.datumIndexed(label);
        // what the datum itself gets wrong is the same for every reference NFT of a shared flat datum: warn once
        if (shared == null || shared.markReported()) {
            warnPropertiesDroppedAsAttack(output, refNft, datumErrors);
        }
        if (!errors.isEmpty()) {
            log.debug("CIP-68 datum of {}/{} (label {}) is indexed with {} validation errors: {}",
                    refNft.policyId(), refNft.assetName(), label, errors.size(), errors);
        }
        if (labels.size() > 1) {
            log.info("CIP-68 reference NFT {}/{} has user tokens for labels {}; the datum is checked against all of them, "
                            + "stored with label {}",
                    refNft.policyId(), refNft.assetName(), labels, label);
        }
        return entity;
    }

    /**
     * A property nested more than 100 levels is dropped as an attack (ADR 0006): one WARN per property names the token and the
     * transaction, so that the attempt can be found afterwards. The parser only counts it, since it does not know either.
     */
    private void warnPropertiesDroppedAsAttack(AddressUtxo output, AssetType refNft, List<ValidationError> datumErrors) {
        for (ValidationError error : datumErrors) {
            if (Cip68DatumParser.TOO_DEEP.equals(error.code())) {
                log.warn("Output {}#{}: dropping property '{}' of the CIP-68 datum of {}/{} and keeping the rest, because its value is "
                                + "nested deeper than {} levels, this looks like an attack",
                        output.getTxHash(), output.getOutputIndex(), error.fieldName(), refNft.policyId(), refNft.assetName(),
                        Cip68DatumParser.MAX_PROPERTY_DEPTH);
            }
        }
    }

    /**
     * Writes the {@code properties} as JSON now, and throws if that fails, so the output is skipped by
     * {@link #skipOutput}.
     * <p>
     * Why: Hibernate writes this column as JSON when the batch is flushed, inside the block transaction. An exception
     * there stops the whole sync, and the datum is untrusted input that anyone can put on-chain: a value nested more
     * than 1000 levels deep did exactly that (https://github.com/bloxbean/yaci-store/issues/1240, a transaction with
     * such a datum was accepted by a real cardano-node). The parser now drops a property nested deeper than 100
     * levels, so this is a second line of defence for a value the parser did not foresee. The general question of
     * isolating any failure of this module from the sync is https://github.com/bloxbean/yaci-store/issues/1243.
     * <p>
     * It uses a default mapper, like Hibernate's JSON mapping when it is not configured; if the two ever differ, this
     * check is only an approximation. It does not replace a transaction-level fix: a failure inside the database at
     * flush or commit is not caught here. Do not remove it because it looks redundant; ask whether #1243 was solved
     * in another way first.
     */
    private void requireSerializableProperties(Cip68Metadata entity) {
        if (entity.getProperties() == null) {
            return;
        }
        try {
            PROPERTIES_MAPPER.writeValueAsString(entity.getProperties());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("the properties cannot be written as JSON: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * The flat datum of an output that holds several reference NFTs. It is the metadata of each of them and is the same
     * for all, so it is parsed once, and what is wrong with it (an undefined version, a wrong type, a Plutus constructor)
     * is counted once for the output instead of once per reference NFT. The rows, the {@code indexed} count and the
     * checks against the labels stay per reference NFT, since each has its own row and its own user tokens.
     */
    private final class SharedDatum {
        private final AddressUtxo output;
        private final int referenceNfts;
        private final ParseOutcome outcome;
        private boolean reported;

        private SharedDatum(AddressUtxo output, int referenceNfts) {
            this.output = output;
            this.referenceNfts = referenceNfts;
            // Parsed here, once, without a reference NFT: the datum is not specific to one of them.
            this.outcome = cip68DatumParser.parseDetailed(output.getInlineDatum(), null);
        }

        /** True the first time it is called: the errors of the shared datum are counted by the first reference NFT. */
        private boolean markReported() {
            boolean first = !reported;
            reported = true;
            return first;
        }
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
     * All the reference NFTs of an output are indexed. An output with one reference NFT is the normal case. With
     * several, a nested (version 4) datum carries the metadata of each, each resolved to its own entry. A flat datum has
     * no entry per token, but the CIP's retrieval steps look up the output of the reference NFT and read its datum,
     * whatever else the output holds, so a flat datum is the metadata of every reference NFT in the output. They are all
     * indexed with it, and one info line lists them so the case can be audited. It is a valid case, so it is not a
     * warning; whatever the shared datum loses is reported as a warning of its own.
     */
    private void logFlatDatumOfSeveralReferenceNfts(AddressUtxo output, List<Amt> refNfts) {
        log.info("Output {}#{} holds {} reference NFTs with a flat CIP-68 datum; CIP-68 gives a flat datum to every "
                        + "reference NFT in the output, so indexing it for all of them: {}",
                output.getTxHash(), output.getOutputIndex(), refNfts.size(),
                refNfts.stream().map(Amt::getUnit).collect(Collectors.joining(", ")));
    }

    private Cip68Metadata buildCip68Metadata(ParsedCip68Datum parsed,
                                             List<ValidationError> validationErrors,
                                             AssetType assetType,
                                             AddressUtxo output,
                                             Long slot,
                                             int label) {
        return Cip68Metadata.builder()
                .policyId(assetType.policyId())
                .assetName(assetType.assetName())
                .slot(slot)
                .txHash(output.getTxHash())
                .txIndex(output.getTxIndex())
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
                .datum(output.getInlineDatum())
                .properties(parsed.properties())
                .validationErrors(validationErrors)
                .lastSyncedAt(LocalDateTime.now())
                .build();
    }
}
