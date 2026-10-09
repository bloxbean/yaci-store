package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Uri;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.DatumRejection;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.Nullable;

import java.math.BigInteger;
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
     * Validate a CIP-68 datum against the fields its label requires.
     *
     * @param parsed the parsed datum
     * @param label  the user-token label the datum belongs to: {@code 222} (NFT), {@code 333} (FT) or
     *               {@code 444} (RFT)
     * @return true if {@link #invalidReason} finds nothing wrong
     */
    public boolean isValidMetadata(ParsedCip68Datum parsed, int label) {
        return invalidReason(parsed, label).isEmpty();
    }

    /**
     * Says why a CIP-68 datum is not indexed, or an empty result when it is valid. Parsing is strict about
     * what CIP-68 requires for the label:
     * <ul>
     *   <li>{@code name} for every label;</li>
     *   <li>{@code description} only for the 333 fungible token (for 222 and 444 it is {@code ? description});</li>
     *   <li>{@code image} for the 222 NFT and the 444 RFT, as a URI whose scheme is one of
     *       {@code https}, {@code ipfs}, {@code ar} or {@code data}. A missing or empty image, or another
     *       scheme (for example {@code iagon://}, or free text), is not indexed: wallets and explorers
     *       follow the CIP and could not show such a token.</li>
     * </ul>
     * The optional fields do not reject the datum: see {@link #invalidLogoReason}.
     *
     * @param parsed the parsed datum
     * @param label  the user-token label the datum belongs to
     * @return the reason the datum is rejected, empty if it is valid
     */
    public Optional<String> invalidReason(ParsedCip68Datum parsed, int label) {
        return rejection(parsed, label).map(DatumRejection::message);
    }

    /**
     * Same check as {@link #invalidReason}, with a machine-readable reason next to the message (used for the
     * skipped-datum counter).
     *
     * @param parsed the parsed datum
     * @param label  the user-token label the datum belongs to
     * @return why the datum is rejected, empty if it is valid
     */
    public Optional<DatumRejection> rejection(ParsedCip68Datum parsed, int label) {
        if (parsed.name() == null) {
            return Optional.of(new DatumRejection(DatumRejection.Reason.NO_NAME, "it has no name"));
        }
        if (label == LABEL_FT) {
            if (parsed.description() == null) {
                return Optional.of(new DatumRejection(DatumRejection.Reason.NO_DESCRIPTION,
                        "it has no description, which CIP-68 requires for a fungible token (label " + LABEL_FT + ")"));
            }
            return Optional.empty();
        }
        String image = parsed.image();
        if (image == null || image.isBlank()) {
            return Optional.of(new DatumRejection(DatumRejection.Reason.NO_IMAGE,
                    "it has no image, which CIP-68 requires for label " + label));
        }
        if (!Cip68Uri.hasAllowedScheme(image)) {
            return Optional.of(new DatumRejection(DatumRejection.Reason.BAD_IMAGE_SCHEME,
                    "its image '" + abbreviate(image) + "' is not a URI with one of the schemes CIP-68 allows "
                            + "(https, ipfs, ar, data)"));
        }
        return Optional.empty();
    }

    /**
     * Says why the {@code logo} of a datum cannot be stored, or an empty result when there is no logo or it is fine.
     * The logo is optional, so a bad one does not reject the datum: it is left out and the rest is indexed. CIP-68
     * defines it as a URI whose scheme is one of {@code https}, {@code ipfs}, {@code ar} or {@code data}, so a bare
     * IPFS hash, or any other scheme, is not accepted.
     *
     * @param parsed the parsed datum
     * @return the reason the logo is left out, empty if there is none or it is valid
     */
    public Optional<String> invalidLogoReason(ParsedCip68Datum parsed) {
        String logo = parsed.logo();
        if (logo == null || logo.isBlank() || Cip68Uri.hasAllowedScheme(logo)) {
            return Optional.empty();
        }
        return Optional.of("its logo '" + abbreviate(logo) + "' is not a URI with one of the schemes CIP-68 allows "
                + "(https, ipfs, ar, data)");
    }

    private static String abbreviate(String value) {
        String oneLine = value.replaceAll("\\s+", " ");
        return oneLine.length() <= 60 ? oneLine : oneLine.substring(0, 60) + "...";
    }

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
     * and project it into a {@link FungibleTokenMetadata}.
     * <p>
     * No label filter is applied: a single reference NFT may have rows tagged with different
     * labels across its history (the per-row label reflects whichever user-token prefix was
     * co-minted in that transaction), so the truthful latest metadata is "max slot for this
     * reference NFT, regardless of label" — see
     * {@link Cip68MetadataRepository#findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc}.
     */
    public Optional<FungibleTokenMetadata> findSubject(String policyId, String assetName, List<String> properties) {
        return metadataReferenceNftRepository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(
                        policyId, assetName)
                .map(referenceNft -> toFungibleTokenMetadata(referenceNft, properties));
    }

    /**
     * Batch variant of {@link #findSubject(String, String, List)}: fetches the latest FT metadata
     * for every given {@code (policyId, assetName)} pair in a single DB round-trip.
     * <p>
     * The input {@code assetName} values are <b>reference NFT</b> asset names (prefix {@code 000643b0}),
     * not fungible token asset names. Callers should use
     * {@link #getReferenceNftSubject(String)} first to map fungible-token subjects to reference-NFT keys.
     * <p>
     * The returned map is keyed by reference-NFT subject ({@code policyId + assetName}) — mirroring the
     * input key shape — so callers can do O(1) lookups without rebuilding keys.
     *
     * @param refNftKeys reference-NFT asset-type pairs (policyId + ref-NFT asset name)
     * @param properties property filter (empty list = include all)
     * @return map keyed by reference-NFT subject; pairs with no data are omitted
     */
    public Map<String, FungibleTokenMetadata> findSubjects(List<AssetType> refNftKeys, List<String> properties) {
        if (refNftKeys.isEmpty()) {
            return Map.of();
        }

        List<String> concatenatedKeys = refNftKeys.stream()
                .map(AssetType::toUnit)
                .toList();

        return metadataReferenceNftRepository.findLatestByConcatenatedKeys(concatenatedKeys).stream()
                .collect(Collectors.toMap(
                        row -> row.getPolicyId() + row.getAssetName(),
                        row -> toFungibleTokenMetadata(row, properties)));
    }

    private FungibleTokenMetadata toFungibleTokenMetadata(Cip68Metadata referenceNft, List<String> properties) {
        return new FungibleTokenMetadata(
                getPropertyIfRequired(Cip68DatumParser.DECIMALS, referenceNft.getDecimals(), properties),
                getPropertyIfRequired(Cip68DatumParser.DESCRIPTION, referenceNft.getDescription(), properties),
                getPropertyIfRequired(Cip68DatumParser.LOGO, referenceNft.getLogo(), properties),
                getPropertyIfRequired(Cip68DatumParser.NAME, referenceNft.getName(), properties),
                getPropertyIfRequired(Cip68DatumParser.TICKER, referenceNft.getTicker(), properties),
                getPropertyIfRequired(Cip68DatumParser.URL, referenceNft.getUrl(), properties),
                getPropertyIfRequired(VERSION, referenceNft.getVersion(), properties));
    }

    @Nullable
    private <T> T getPropertyIfRequired(String propertyName, T propertyValue, List<String> properties) {
        if (properties.isEmpty() || properties.contains(propertyName)) {
            return propertyValue;
        }

        return null;
    }

    /**
     * Converts a fungible token subject to its corresponding reference NFT subject.
     * <p>
     * CIP-68 stores metadata in a <b>reference NFT</b> (label 100, prefix {@code 000643b0}),
     * not in the fungible token itself (label 333, prefix {@code 0014df10}). Both tokens are
     * minted together under the same policy with the same base name — only the 8-char label
     * prefix differs.
     * <p>
     * When a user queries by the fungible token subject they see in their wallet
     * (e.g. {@code <policyId>0014df10<name>}), this method swaps the prefix to produce the
     * reference NFT subject ({@code <policyId>000643b0<name>}) that the database stores.
     * <p>
     * Returns empty if the subject does not have a fungible token prefix ({@code 0014df10}),
     * indicating the subject is not a CIP-68 fungible token.
     *
     * @param subject the fungible token unit (policyId + 0014df10 + hex name)
     * @return the reference NFT AssetType if the subject has a fungible token prefix, empty otherwise
     */
    public Optional<AssetType> getReferenceNftSubject(String subject) {
        AssetType assetType = AssetType.fromUnit(subject);
        String assetName = assetType.assetName();
        int tokenPrefixLength = REFERENCE_TOKEN_PREFIX.length();

        if (assetName.length() > tokenPrefixLength && assetName.startsWith(FUNGIBLE_TOKEN_PREFIX)) {
            String refNftAssetName = String.format("%s%s", REFERENCE_TOKEN_PREFIX, assetType.assetName().substring(tokenPrefixLength));
            return Optional.of(new AssetType(assetType.policyId(), refNftAssetName));
        }

        return Optional.empty();
    }

}
