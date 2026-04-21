package com.bloxbean.cardano.yaci.store.blockfrost.scripts.mapper;

import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFDatum;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScript;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptListItem;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptRedeemer;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
public abstract class BFScriptsMapperDecorator implements BFScriptsMapper {

    @Autowired
    @Qualifier("delegate")
    private BFScriptsMapper delegate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String NATIVE_SCRIPT = "NATIVE_SCRIPT";

    @Override
    public BFScriptListItemDto toListItemDto(BFScriptListItem model) {
        return delegate.toListItemDto(model);
    }

    @Override
    public BFScriptDto toScriptDto(BFScript model) {
        return BFScriptDto.builder()
                .scriptHash(model.scriptHash())
                .type(mapScriptType(model.scriptType()))
                .serialisedSize(computeSerialisedSize(model))
                .build();
    }

    @Override
    public BFScriptJsonDto toScriptJsonDto(BFScript model) {
        // Plutus scripts have no JSON representation — return null (matches BF exactly, not 404)
        if (!NATIVE_SCRIPT.equalsIgnoreCase(model.scriptType())) {
            return BFScriptJsonDto.builder().json(null).build();
        }

        String rawContent = model.content();
        if (rawContent == null || rawContent.isBlank()) {
            return BFScriptJsonDto.builder().json(null).build();
        }

        // Native script content is stored as JSON: {"content": "<json-string>"}
        // The inner "content" field holds the actual native script JSON
        try {
            JsonNode contentNode = JsonUtil.parseJson(rawContent);
            if (contentNode != null && contentNode.has("content")) {
                String innerContent = contentNode.get("content").asText();
                Object parsed = objectMapper.readValue(innerContent, Object.class);
                return BFScriptJsonDto.builder().json(parsed).build();
            }
            // Fallback: try parsing the whole value
            Object parsed = objectMapper.readValue(rawContent, Object.class);
            return BFScriptJsonDto.builder().json(parsed).build();
        } catch (Exception e) {
            log.warn("Failed to parse native script JSON for hash {}: {}", model.scriptHash(), e.getMessage());
            return BFScriptJsonDto.builder().json(null).build();
        }
    }

    @Override
    public BFScriptCborDto toScriptCborDto(BFScript model) {
        // Native scripts have no CBOR — return null (matches BF exactly, not 404)
        if (NATIVE_SCRIPT.equalsIgnoreCase(model.scriptType())) {
            return BFScriptCborDto.builder().cbor(null).build();
        }

        String rawContent = model.content();
        if (rawContent == null || rawContent.isBlank()) {
            return BFScriptCborDto.builder().cbor(null).build();
        }

        // Plutus script content is stored as JSON: {"type":"PlutusScriptVx","content":"<cbor-hex>"}
        // The "content" hex is double-wrapped: CBOR_bytestring(serialised_script).
        // Blockfrost returns only the inner serialised_script (the single-wrapped form).
        // We must strip the outer CBOR bytestring header dynamically (header size varies by payload length).
        try {
            JsonNode contentNode = JsonUtil.parseJson(rawContent);
            if (contentNode != null && contentNode.has("content")) {
                String outerHex = contentNode.get("content").asText();
                String strippedHex = stripOuterCborByteStringHeader(outerHex);
                return BFScriptCborDto.builder().cbor(strippedHex).build();
            }
        } catch (Exception e) {
            log.warn("Failed to extract CBOR for script hash {}: {}", model.scriptHash(), e.getMessage());
        }

        return BFScriptCborDto.builder().cbor(null).build();
    }

    @Override
    public BFScriptRedeemerDto toRedeemerDto(BFScriptRedeemer model) {
        return BFScriptRedeemerDto.builder()
                .txHash(model.txHash())
                .txIndex(model.txIndex() != null ? model.txIndex() : 0)
                .purpose(mapPurpose(model.purpose()))
                .unitMem(model.unitMem() != null ? String.valueOf(model.unitMem()) : "0")
                .unitSteps(model.unitSteps() != null ? String.valueOf(model.unitSteps()) : "0")
                .fee(model.fee() != null ? model.fee() : "0")
                .redeemerDataHash(model.redeemerDataHash())
                .datumHash(model.redeemerDataHash())
                .build();
    }

    @Override
    public BFDatumDto toDatumDto(BFDatum model) {
        String cborHex = model.cborHex();
        if (cborHex == null || cborHex.isBlank()) {
            return BFDatumDto.builder().jsonValue(null).build();
        }

        // datum.datum stores CBOR hex — convert to JSON via PlutusData
        try {
            PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(cborHex));
            String jsonStr = PlutusDataJsonConverter.toJson(plutusData);
            Object parsed = objectMapper.readValue(jsonStr, Object.class);
            return BFDatumDto.builder().jsonValue(parsed).build();
        } catch (Exception e) {
            log.warn("Failed to convert datum CBOR to JSON for hash {}: {}", model.hash(), e.getMessage());
            return BFDatumDto.builder().jsonValue(null).build();
        }
    }

    @Override
    public BFDatumCborDto toDatumCborDto(BFDatum model) {
        return delegate.toDatumCborDto(model);
    }

    // ---- helpers ----

    /**
     * Computes the serialised script size in bytes.
     * <p>
     * The {@code content} column stores JSON like {@code {"type":"PlutusScriptV2","content":"<outer-cbor-hex>"}}.
     * The CBOR hex is double-wrapped: the outer bytes are a CBOR bytestring whose payload is the
     * serialised script.  Blockfrost returns the payload length as {@code serialised_size}.
     * <p>
     * The CBOR bytestring header is variable-length (see {@link #stripOuterCborByteStringHeader}),
     * so we strip it dynamically and measure the remaining bytes.
     * <p>
     * Native scripts return {@code null} (Blockfrost does the same).
     */
    private Integer computeSerialisedSize(BFScript model) {
        if (NATIVE_SCRIPT.equalsIgnoreCase(model.scriptType())) {
            return null; // native scripts: BF returns null
        }
        String rawContent = model.content();
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }
        try {
            JsonNode contentNode = JsonUtil.parseJson(rawContent);
            if (contentNode != null && contentNode.has("content")) {
                String outerHex = contentNode.get("content").asText();
                if (outerHex != null && !outerHex.isBlank()) {
                    String innerHex = stripOuterCborByteStringHeader(outerHex);
                    int bytes = innerHex.length() / 2;
                    return bytes > 0 ? bytes : null;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to compute serialised_size for script hash {}: {}", model.scriptHash(), e.getMessage());
        }
        return null;
    }

    /**
     * Strips the outer CBOR major-type-2 (byte string) header from a hex-encoded CBOR value,
     * returning the inner payload hex.
     *
     * <p>CBOR byte-string header layout:
     * <ul>
     *   <li>First byte high 3 bits = 010 (major type 2 = byte string)</li>
     *   <li>Low 5 bits = additional info:
     *     <ul>
     *       <li>0–23  → inline length, 1-byte header total</li>
     *       <li>24    → 1-byte length follows, 2-byte header total</li>
     *       <li>25    → 2-byte length follows, 3-byte header total</li>
     *       <li>26    → 4-byte length follows, 5-byte header total</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param outerHex hex string of a CBOR-encoded byte string (double-wrapped Plutus script)
     * @return hex string of the inner payload (the serialised script returned by Blockfrost)
     * @throws IllegalArgumentException if the first byte is not a CBOR byte string
     */
    private String stripOuterCborByteStringHeader(String outerHex) {
        if (outerHex == null || outerHex.length() < 2) {
            throw new IllegalArgumentException("CBOR hex too short: " + outerHex);
        }
        int firstByte = Integer.parseInt(outerHex.substring(0, 2), 16);
        int majorType = (firstByte >> 5) & 0x7;
        if (majorType != 2) {
            throw new IllegalArgumentException(
                    "Expected CBOR major type 2 (byte string), got " + majorType
                            + " in hex: " + outerHex.substring(0, Math.min(8, outerHex.length())));
        }
        int additional = firstByte & 0x1F;
        int headerHexChars;
        if (additional <= 23) {
            // Inline length — 1-byte header (2 hex chars)
            headerHexChars = 2;
        } else if (additional == 24) {
            // 1-byte length follows — 2-byte header (4 hex chars)
            headerHexChars = 4;
        } else if (additional == 25) {
            // 2-byte length follows — 3-byte header (6 hex chars)
            headerHexChars = 6;
        } else if (additional == 26) {
            // 4-byte length follows — 5-byte header (10 hex chars)
            headerHexChars = 10;
        } else {
            throw new IllegalArgumentException("Unsupported CBOR additional info: " + additional);
        }
        return outerHex.substring(headerHexChars);
    }

    private String mapScriptType(String dbType) {
        if (dbType == null) return "unknown";
        return switch (dbType.toUpperCase()) {
            case "NATIVE_SCRIPT" -> "timelock";
            case "PLUTUS_V1"     -> "plutusV1";
            case "PLUTUS_V2"     -> "plutusV2";
            case "PLUTUS_V3"     -> "plutusV3";
            default              -> dbType.toLowerCase();
        };
    }

    private String mapPurpose(String purpose) {
        if (purpose == null) return null;
        return switch (purpose.toUpperCase()) {
            case "SPEND"  -> "spend";
            case "MINT"   -> "mint";
            case "CERT"   -> "cert";
            case "REWARD" -> "reward";
            default       -> purpose.toLowerCase();
        };
    }
}
