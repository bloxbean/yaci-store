package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser;

import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ListPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Parses CIP-113 registry node inline datums.
 * <p>
 * A registry node datum is a ConstrPlutusData (or list) with 5 fields:
 * <ol>
 *   <li>key — ByteString (policy ID of registered token, required)</li>
 *   <li>next — ByteString (next pointer in sorted linked list, required)</li>
 *   <li>transferLogicScript — ConstrPlutusData wrapping a ByteString credential (optional)</li>
 *   <li>thirdPartyTransferLogicScript — ConstrPlutusData wrapping a ByteString credential (optional)</li>
 *   <li>globalStatePolicyId — ByteString (optional, may be absent or empty)</li>
 * </ol>
 */
@Component
@Slf4j
public class Cip113RegistryNodeParser {

    /**
     * Parsed registry node data. {@code key} and {@code next} are required by the CIP-113
     * linked list structure. All three script/policy fields are optional.
     */
    public record ParsedRegistryNode(String key,
                                     String next,
                                     @Nullable String transferLogicScript,
                                     @Nullable String thirdPartyTransferLogicScript,
                                     @Nullable String globalStatePolicyId) {
    }

    public Optional<ParsedRegistryNode> parse(String inlineDatum) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return Optional.empty();
        }

        try {
            PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

            List<PlutusData> fields = extractFields(plutusData);
            if (fields.size() < 4) {
                log.warn("CIP-113 registry node datum has insufficient fields: {}", fields.size());
                return Optional.empty();
            }

            String key = extractBytes(fields.get(0));
            String next = extractBytes(fields.get(1));
            String transferLogicScript = extractCredentialBytesOrNull(fields.get(2));
            String thirdPartyTransferLogicScript = extractCredentialBytesOrNull(fields.get(3));
            String globalStatePolicyId = fields.size() > 4 ? extractBytesOrNull(fields.get(4)) : null;

            if (key == null || next == null) {
                log.warn("Skipping invalid CIP-113 registry node: key={}, next={} — "
                                + "both fields are required by the on-chain linked list structure",
                        key, next);
                return Optional.empty();
            }

            return Optional.of(new ParsedRegistryNode(key, next, transferLogicScript,
                    thirdPartyTransferLogicScript, globalStatePolicyId));

        } catch (Exception e) {
            log.warn("Failed to parse CIP-113 registry node datum: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private List<PlutusData> extractFields(PlutusData plutusData) {
        if (plutusData instanceof ConstrPlutusData constr) {
            return constr.getData().getPlutusDataList();
        } else if (plutusData instanceof ListPlutusData list) {
            return list.getPlutusDataList();
        }
        return List.of();
    }

    @Nullable
    private String extractBytes(PlutusData data) {
        if (data instanceof BytesPlutusData bytes) {
            return HexUtil.encodeHexString(bytes.getValue());
        }
        return null;
    }

    @Nullable
    private String extractBytesOrNull(PlutusData data) {
        String hex = extractBytes(data);
        return (hex == null || hex.isEmpty()) ? null : hex;
    }

    /**
     * Credentials are wrapped in a ConstrPlutusData: Constr(0, [ByteString]).
     * Returns the inner byte string or null if the structure is invalid.
     */
    @Nullable
    private String extractCredentialBytes(PlutusData data) {
        if (data instanceof ConstrPlutusData constr) {
            List<PlutusData> innerFields = constr.getData().getPlutusDataList();
            if (!innerFields.isEmpty()) {
                return extractBytes(innerFields.getFirst());
            }
        }
        return null;
    }

    /**
     * Like {@link #extractCredentialBytes} but returns null for empty credentials.
     * All three credential/script fields in a CIP-113 registry node are optional.
     */
    @Nullable
    private String extractCredentialBytesOrNull(PlutusData data) {
        if (data instanceof BytesPlutusData bytes && bytes.getValue().length == 0) {
            return null;
        }
        return extractCredentialBytes(data);
    }

}
