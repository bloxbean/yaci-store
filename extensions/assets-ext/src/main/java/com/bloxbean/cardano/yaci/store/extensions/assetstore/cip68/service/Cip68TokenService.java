package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.Nullable;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68TokenService {

    private static final String VERSION = "version";

    private final Cip68MetadataRepository metadataReferenceNftRepository;

    /**
     * Checks whether the utxo contains an NFT which matches Cip68 Reference NFT requirements
     *
     * @param utxo the utxo to check
     * @return true if any of the utxo's contains a Cip68 Reference NFT
     */
    public boolean containsReferenceNft(AddressUtxo utxo) {
        return utxo.getAmounts().stream().anyMatch(this::isReferenceNft);
    }

    /**
     * Checks and returns an NFT which matches Cip68 Reference NFT requirements if present
     *
     * @param utxo the utxo to check
     * @return the amt matching the Reference NFT if found
     */
    public Optional<Amt> extractReferenceNft(AddressUtxo utxo) {
        return utxo.getAmounts().stream().filter(this::isReferenceNft).findFirst();
    }

    /**
     * Returns every reference NFT in the output, in the order of its assets. An output normally holds one,
     * but a version 4 nested datum can describe several, which is why they may be locked together.
     *
     * @param utxo the utxo to check
     * @return the amounts matching Cip68 Reference NFT requirements, empty if there are none
     */
    public List<Amt> extractReferenceNfts(AddressUtxo utxo) {
        return utxo.getAmounts().stream().filter(this::isReferenceNft).toList();
    }

    /**
     * Check if the amount matches Cip68 Reference NFT Requirements
     *
     * @param amount the amount to check
     * @return true if the amount is a Cip68 Reference NFT
     */
    public boolean isReferenceNft(Amt amount) {
        return amount.getQuantity().equals(BigInteger.ONE)
                && AssetType.fromUnit(amount.getUnit()).assetName().startsWith(REFERENCE_TOKEN_PREFIX);
    }

    /**
     * Find the latest reference-NFT metadata for the given {@code (policyId, assetName)} pair
     * and project it into a {@link FungibleTokenMetadata} for a token of {@code label}.
     * <p>
     * No label filter is applied: a single reference NFT may have rows tagged with different
     * labels across its history (the per-row label reflects whichever user-token prefix was
     * co-minted in that transaction, or was guessed from the datum), so the truthful latest metadata is
     * "max slot for this reference NFT, regardless of label" — see
     * {@link Cip68MetadataRepository#findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc}.
     * The label of the token asked for decides what CIP-68 requires of it and whether its image is its logo.
     *
     * @param label the label of the token asked for: {@code 333} (FT) or {@code 444} (RFT)
     */
    public Optional<FungibleTokenMetadata> findSubject(String policyId, String assetName, int label, List<String> properties) {
        return metadataReferenceNftRepository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(
                        policyId, assetName)
                .map(referenceNft -> toFungibleTokenMetadata(referenceNft, label, properties));
    }

    /**
     * Batch variant of {@link #findSubject}: fetches the latest metadata of every fungible (333) or rich fungible (444)
     * token subject in a single DB round-trip. A 333 and a 444 subject of the same reference NFT are both served from
     * its one row, each checked for its own label.
     *
     * @param subjects   user-token subjects (policyId + {@code 0014df10} or {@code 001bc280} + hex name); any other
     *                   subject is skipped
     * @param properties property filter (empty list = include all)
     * @return map keyed by the subject asked for; subjects with no data are omitted
     */
    public Map<String, FungibleTokenMetadata> findSubjects(List<String> subjects, List<String> properties) {
        Map<String, AssetType> refNftBySubject = new LinkedHashMap<>();
        for (String subject : subjects) {
            getReferenceNftSubject(subject).ifPresent(refNft -> refNftBySubject.put(subject, refNft));
        }
        if (refNftBySubject.isEmpty()) {
            return Map.of();
        }

        List<String> concatenatedKeys = refNftBySubject.values().stream()
                .map(AssetType::toUnit)
                .distinct()
                .toList();
        Map<String, Cip68Metadata> rowByRefNft = metadataReferenceNftRepository.findLatestByConcatenatedKeys(concatenatedKeys)
                .stream()
                .collect(Collectors.toMap(row -> row.getPolicyId() + row.getAssetName(), row -> row));

        Map<String, FungibleTokenMetadata> result = new HashMap<>();
        refNftBySubject.forEach((subject, refNft) -> {
            Cip68Metadata row = rowByRefNft.get(refNft.toUnit());
            if (row != null) {
                result.put(subject, toFungibleTokenMetadata(row, labelOf(subject), properties));
            }
        });
        return result;
    }

    /**
     * The row served as a token of {@code label}. Its errors are what is wrong with the datum, stored when it was
     * indexed, and what the label requires of it ({@link Cip68Validator#validate}); a row indexed before validation
     * existed keeps {@code null}: it was never checked.
     */
    private FungibleTokenMetadata toFungibleTokenMetadata(Cip68Metadata referenceNft, int label, List<String> properties) {
        List<ValidationError> errors = referenceNft.getValidationErrors() == null
                ? null
                : Cip68Validator.validate(referenceNft, label);
        return new FungibleTokenMetadata(
                getPropertyIfRequired(Cip68DatumParser.DECIMALS, referenceNft.getDecimals(), properties),
                getPropertyIfRequired(Cip68DatumParser.DESCRIPTION, referenceNft.getDescription(), properties),
                getPropertyIfRequired(Cip68DatumParser.LOGO, logoOf(referenceNft, label), properties),
                getPropertyIfRequired(Cip68DatumParser.NAME, referenceNft.getName(), properties),
                getPropertyIfRequired(Cip68DatumParser.TICKER, referenceNft.getTicker(), properties),
                getPropertyIfRequired(Cip68DatumParser.URL, referenceNft.getUrl(), properties),
                getPropertyIfRequired(VERSION, referenceNft.getVersion(), properties),
                errors);
    }

    /**
     * The logo to serve. A rich fungible token (444) has no {@code logo} in CIP-68: its picture is the {@code image},
     * which is served as the logo, as cf-token-metadata-registry does, so that wallets show it unchanged. A {@code logo}
     * in its datum is not part of CIP-68 for a 444, so it is not served, also when there is no image. Whether the token
     * is an RFT is the label of the subject asked for, not the label the row was stored with, which may have been
     * guessed. The image is served as written even when it is not a URI with an allowed scheme; the validation errors
     * say so.
     */
    @Nullable
    private static String logoOf(Cip68Metadata row, int label) {
        return label == LABEL_RFT ? row.getImage() : row.getLogo();
    }

    /** The label of a fungible ({@code 0014df10}) or rich fungible ({@code 001bc280}) token subject. */
    private static int labelOf(String subject) {
        return AssetType.fromUnit(subject).assetName().startsWith(RICH_FUNGIBLE_TOKEN_PREFIX) ? LABEL_RFT : LABEL_FT;
    }

    /**
     * The reference NFT of a fungible (333) or rich fungible (444) token subject, and the label of that subject; empty
     * for any other subject.
     */
    public Optional<ServedToken> getServedToken(String subject) {
        return getReferenceNftSubject(subject).map(refNft -> new ServedToken(refNft, labelOf(subject)));
    }

    /**
     * A user-token subject that is served from CIP-68: its reference NFT, and its label.
     *
     * @param referenceNft the reference NFT (prefix {@code 000643b0}) the metadata is read from
     * @param label        {@code 333} (FT) or {@code 444} (RFT)
     */
    public record ServedToken(AssetType referenceNft, int label) {
    }

    @Nullable
    private <T> T getPropertyIfRequired(String propertyName, T propertyValue, List<String> properties) {
        if (properties.isEmpty() || properties.contains(propertyName)) {
            return propertyValue;
        }

        return null;
    }

    /**
     * Converts a fungible or rich fungible token subject to its corresponding reference NFT subject.
     * <p>
     * CIP-68 stores metadata in a <b>reference NFT</b> (label 100, prefix {@code 000643b0}),
     * not in the user token itself (label 333, prefix {@code 0014df10}, or label 444, prefix
     * {@code 001bc280}). Both tokens are minted together under the same policy with the same base
     * name — only the 8-char label prefix differs. NFTs (label 222) are not served.
     * <p>
     * When a user queries by the fungible token subject they see in their wallet
     * (e.g. {@code <policyId>0014df10<name>}), this method swaps the prefix to produce the
     * reference NFT subject ({@code <policyId>000643b0<name>}) that the database stores.
     * <p>
     * Returns empty if the subject has neither the fungible token prefix ({@code 0014df10}) nor the
     * rich fungible token prefix ({@code 001bc280}), indicating the subject is not served from CIP-68.
     *
     * @param subject the user token unit (policyId + 0014df10 or 001bc280 + hex name)
     * @return the reference NFT AssetType if the subject has a served token prefix, empty otherwise
     */
    public Optional<AssetType> getReferenceNftSubject(String subject) {
        AssetType assetType = AssetType.fromUnit(subject);
        String assetName = assetType.assetName();
        int tokenPrefixLength = REFERENCE_TOKEN_PREFIX.length();

        if (assetName.length() > tokenPrefixLength
                && (assetName.startsWith(FUNGIBLE_TOKEN_PREFIX) || assetName.startsWith(RICH_FUNGIBLE_TOKEN_PREFIX))) {
            String refNftAssetName = String.format("%s%s", REFERENCE_TOKEN_PREFIX, assetType.assetName().substring(tokenPrefixLength));
            return Optional.of(new AssetType(assetType.policyId(), refNftAssetName));
        }

        return Optional.empty();
    }

}
