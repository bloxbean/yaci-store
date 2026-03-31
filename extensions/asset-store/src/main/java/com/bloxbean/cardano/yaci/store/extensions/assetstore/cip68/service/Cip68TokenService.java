package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.MetadataReferenceNftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68TokenService {

    private static final String VERSION = "version";

    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    /**
     * In order to be a valid FT Token Metadata Reference datum there are some constraints (name and description must be present)
     *
     * @return true if the metadata are compliant to the FT Cip68 standard
     */
    public boolean isValidMetadata(FungibleTokenMetadata cip68TokenMetadata) {
        return cip68TokenMetadata.name() != null && cip68TokenMetadata.description() != null;
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
     * Find fungible token metadata by policy ID and asset name.
     * Uses label-aware query to only return FT (label 333) entries.
     */
    public Optional<FungibleTokenMetadata> findSubject(String policyId, String assetName, List<String> properties) {
        return metadataReferenceNftRepository.findFirstByPolicyIdAndAssetNameAndLabelOrderBySlotDesc(
                        policyId, assetName, LABEL_FT)
                .map(referenceNft -> new FungibleTokenMetadata(getPropertyIfRequired(Cip68DatumParser.DECIMALS, referenceNft.getDecimals(), properties),
                        getPropertyIfRequired(Cip68DatumParser.DESCRIPTION, referenceNft.getDescription(), properties),
                        getPropertyIfRequired(Cip68DatumParser.LOGO, referenceNft.getLogo(), properties),
                        getPropertyIfRequired(Cip68DatumParser.NAME, referenceNft.getName(), properties),
                        getPropertyIfRequired(Cip68DatumParser.TICKER, referenceNft.getTicker(), properties),
                        getPropertyIfRequired(Cip68DatumParser.URL, referenceNft.getUrl(), properties),
                        getPropertyIfRequired(VERSION, referenceNft.getVersion(), properties)));
    }

    private <T> T getPropertyIfRequired(String propertyName, T propertyValue, List<String> properties) {
        if (properties.isEmpty() || properties.contains(propertyName)) {
            return propertyValue;
        } else {
            return null;
        }
    }

    /**
     * Converts a fungible token subject (prefix 0014df10) to its corresponding reference NFT subject (prefix 000643b0).
     * Returns empty if the subject does not have a fungible token prefix.
     */
    public Optional<AssetType> getReferenceNftSubject(String subject) {
        AssetType assetType = AssetType.fromUnit(subject);
        String assetName = assetType.assetName();
        int tokenPrefixLength = REFERENCE_TOKEN_PREFIX.length();
        if (assetName.length() > tokenPrefixLength && assetName.startsWith(FUNGIBLE_TOKEN_PREFIX)) {
            String refNftAssetName = String.format("%s%s", REFERENCE_TOKEN_PREFIX, assetType.assetName().substring(tokenPrefixLength));
            return Optional.of(new AssetType(assetType.policyId(), refNftAssetName));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Derives the CIP-68 label from a reference NFT asset name prefix.
     * The reference NFT always has prefix 000643b0 (label 100), but the corresponding
     * user token label determines the token type:
     * <ul>
     *   <li>If the reference NFT was minted alongside a token with prefix 0014df10 → label 333 (FT)</li>
     *   <li>If alongside prefix 000de140 → label 222 (NFT)</li>
     *   <li>If alongside prefix 001bc280 → label 444 (RFT)</li>
     * </ul>
     * Since we currently only index FTs, this always returns LABEL_FT.
     * When NFT support is added, this should inspect the minting transaction
     * to determine which user token prefix was minted alongside the reference NFT.
     */
    public static int deriveLabelForReferenceNft() {
        // TODO: When NFT support is added, derive label from co-minted user token prefix
        return LABEL_FT;
    }

}
