package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.client.metadata.MetadataBuilder;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ConversionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP utility service for Cardano data conversions.
 * Provides tools to convert CBOR to JSON for metadata and datums.
 * Essential for developers debugging and analyzing on-chain data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpCardanoUtilService {

    @Tool(name = "convert-metadata-cbor-to-json",
          description = "Convert transaction metadata CBOR hex to readable JSON format. " +
                       "Input should be metadata CBOR in hex format. " +
                       "Uses Cardano Client Library's MetadataBuilder to deserialize and convert. " +
                       "Useful for debugging metadata, analyzing NFT metadata, or verifying metadata structure. " +
                       "Returns both the original CBOR and converted JSON, plus success status.")
    public ConversionResult convertMetadataCborToJson(
        @ToolParam(description = "Metadata CBOR in hex format") String cborHex
    ) {
        log.debug("Converting metadata CBOR to JSON: {} chars", cborHex != null ? cborHex.length() : 0);

        if (cborHex == null || cborHex.trim().isEmpty()) {
            return ConversionResult.failure("", "metadata", "Input CBOR hex is null or empty");
        }

        try {
            // Decode hex and deserialize CBOR
            byte[] cborBytes = HexUtil.decodeHexString(cborHex);
            CBORMetadata metadata = CBORMetadata.deserialize(cborBytes);

            // Convert to JSON
            String json = MetadataBuilder.toJson(metadata);

            return ConversionResult.success(cborHex, json, "metadata");
        } catch (Exception e) {
            log.warn("Failed to convert metadata CBOR to JSON", e);
            return ConversionResult.failure(cborHex, "metadata",
                "Conversion failed: " + e.getMessage());
        }
    }

    @Tool(name = "convert-datum-cbor-to-json",
          description = "Convert Plutus datum CBOR hex to readable JSON format. " +
                       "Input should be datum CBOR in hex format (as stored in datum table). " +
                       "Uses Cardano Client Library's PlutusData deserializer and JSON converter. " +
                       "Useful for debugging smart contracts, analyzing datum structure, or verifying contract parameters. " +
                       "Returns both the original CBOR and converted JSON, plus success status.")
    public ConversionResult convertDatumCborToJson(
        @ToolParam(description = "Datum CBOR in hex format") String cborHex
    ) {
        log.debug("Converting datum CBOR to JSON: {} chars", cborHex != null ? cborHex.length() : 0);

        if (cborHex == null || cborHex.trim().isEmpty()) {
            return ConversionResult.failure("", "datum", "Input CBOR hex is null or empty");
        }

        try {
            // Decode hex and deserialize Plutus data
            byte[] cborBytes = HexUtil.decodeHexString(cborHex);
            PlutusData plutusData = PlutusData.deserialize(cborBytes);

            // Convert to JSON
            String json = PlutusDataJsonConverter.toJson(plutusData);

            return ConversionResult.success(cborHex, json, "datum");
        } catch (Exception e) {
            log.warn("Failed to convert datum CBOR to JSON", e);
            return ConversionResult.failure(cborHex, "datum",
                "Conversion failed: " + e.getMessage());
        }
    }
}
