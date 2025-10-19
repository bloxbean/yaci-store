package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.metadata.MetadataBuilder;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.mcp.server.model.CardanoNetworkInfo;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ConversionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP utility service for Cardano data conversions and unit conventions.
 * Provides tools to convert CBOR to JSON for metadata and datums, and explains Cardano amount units.
 * Essential for developers debugging and analyzing on-chain data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpCardanoUtilService {
    private final GenesisConfig genesisConfig;
    private final StoreProperties storeProperties;

    @Tool(name = "cardano-network-info",
          description = "⚠️ CRITICAL: Read this FIRST when calculating slot ranges or time periods! " +
                       "Provides network genesis configuration including slot duration and epoch length. " +
                       "ESSENTIAL for correctly converting time ranges to slot numbers. " +
                       "ALWAYS call this tool before querying data by slot ranges (e.g., 'last 24 hours'). " +
                       "Prevents slot calculation errors that cause incorrect time range queries.")
    public CardanoNetworkInfo getCardanoNetworkInfo() {
        long protocolMagic = storeProperties.getProtocolMagic();
        NetworkType networkType = NetworkType.fromProtocolMagic(protocolMagic);

        double shelleySlotLength = genesisConfig.slotDuration(Era.Shelley);
        long byronSlotLength = (long) genesisConfig.slotDuration(Era.Byron);
        long epochLength = genesisConfig.getEpochLength();
        long startTime = genesisConfig.getStartTime(protocolMagic);

        return CardanoNetworkInfo.create(
            networkType.name(),
            protocolMagic,
            shelleySlotLength,
            byronSlotLength,
            epochLength,
            startTime
        );
    }

    @Tool(name = "cardano-amount-units-info",
          description = "⚠️ IMPORTANT: Read this FIRST when working with Cardano monetary amounts! " +
                       "Explains the unit conventions (lovelace vs ADA) used across ALL Yaci Store MCP tools. " +
                       "This is CRITICAL for correctly interpreting and presenting monetary values to users. " +
                       "ALWAYS call this tool before presenting any balance, fee, stake, or reward amounts.")
    public String getAmountUnitsInfo() {
        return """
            ════════════════════════════════════════════════════════════════
            CARDANO AMOUNT UNITS - READ THIS FIRST!
            ════════════════════════════════════════════════════════════════

            ALL MONETARY AMOUNTS IN YACI STORE ARE IN LOVELACE (NOT ADA!)

            1 ADA = 1,000,000 lovelace

            ════════════════════════════════════════════════════════════════
            WHAT IS LOVELACE?
            ════════════════════════════════════════════════════════════════
            Lovelace is the smallest unit of ADA (like satoshis for Bitcoin).
            All blockchain amounts are stored in lovelace for precision.

            ════════════════════════════════════════════════════════════════
            AFFECTED FIELDS (ALL IN LOVELACE):
            ════════════════════════════════════════════════════════════════
            - balance, amount, value
            - fee, total_fees
            - deposit, total_deposit
            - stake, total_stake, delegated_stake
            - reward, total_rewards
            - voting_power, drep_voting_power, spo_stake
            - treasury amounts, withdrawal amounts

            ════════════════════════════════════════════════════════════════
            CONVERSION FORMULAS:
            ════════════════════════════════════════════════════════════════
            Lovelace → ADA: divide by 1,000,000
            ADA → Lovelace: multiply by 1,000,000

            Examples:
            - 5000000 lovelace = 5 ADA
            - 1500000000 lovelace = 1,500 ADA
            - 250000 lovelace = 0.25 ADA

            ════════════════════════════════════════════════════════════════
            IMPORTANT EXCEPTIONS:
            ════════════════════════════════════════════════════════════════
            - Native tokens/assets: Use their own decimals (check metadata)
            - NFTs: Usually 0 decimals (quantity = actual count)
            - Token amounts: NOT in lovelace (only ADA is in lovelace)

            ════════════════════════════════════════════════════════════════
            GOLDEN RULE:
            ════════════════════════════════════════════════════════════════
            When presenting ANY amount to users:
            1. Check if it's ADA (lovelace) or a native token
            2. If ADA: ALWAYS convert lovelace to ADA (÷ 1,000,000)
            3. If token: Check token metadata for decimals
            4. Display with clear unit label (e.g., "5.0 ADA" not "5000000")

            ════════════════════════════════════════════════════════════════
            """;
    }

    @Tool(name = "convert-lovelace-to-ada",
          description = "Convert lovelace amount to ADA with proper decimal formatting. " +
                       "Use this whenever you need to present lovelace amounts to users. " +
                       "Returns formatted string with ADA unit label.")
    public String convertLovelaceToAda(
        @ToolParam(description = "Amount in lovelace") long lovelace
    ) {
        double ada = lovelace / 1_000_000.0;
        return String.format("%.6f ADA (%,d lovelace)", ada, lovelace);
    }

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

    @Tool(name = "extract-stake-address",
          description = "Extract the stake address from a Cardano address (bech32 format). " +
                       "Supports different address types: " +
                       "- Base address (addr1...): Returns the associated stake address (stake1...) " +
                       "- Stake address (stake1...): Returns the same stake address " +
                       "- Enterprise address (addr1...): Returns null (no stake component) " +
                       "- Pointer address: Returns null (uses on-chain pointer, not embedded stake) " +
                       "Useful for finding which stake address is associated with a payment address, " +
                       "or for querying stake-related data (delegation, rewards) for an address.")
    public String extractStakeAddress(
        @ToolParam(description = "Cardano address in bech32 format (addr1... or stake1...)") String address
    ) {
        log.debug("Extracting stake address from: {}", address);

        if (address == null || address.trim().isEmpty()) {
            log.warn("Input address is null or empty");
            return null;
        }

        try {
            // Parse the address
            Address addr = new Address(address);

            // If it's already a stake address, return it
            if (address.startsWith("stake")) {
                log.debug("Input is already a stake address: {}", address);
                return address;
            }

            // Try to get the delegation credential (stake part) from the address
            var delegationCredential = addr.getDelegationCredential();

            if (delegationCredential.isEmpty()) {
                log.debug("Address has no stake component (enterprise or pointer address): {}", address);
                return null;
            }

            // Create stake address from the delegation credential
            String stakeAddress = AddressProvider.getRewardAddress(
                delegationCredential.get(),
                addr.getNetwork()
            ).toBech32();

            log.debug("Extracted stake address: {} from base address: {}", stakeAddress, address);
            return stakeAddress;

        } catch (Exception e) {
            log.warn("Failed to extract stake address from: {}", address, e);
            return null;
        }
    }

    @Tool(name = "script-hash-to-address",
          description = "Convert a script hash to a Cardano enterprise address (bech32 format). " +
                       "Enterprise addresses contain only the payment credential (no staking component). " +
                       "IMPORTANT: Requires network specification - mainnet vs testnet addresses have different prefixes. " +
                       "Returns: " +
                       "- Mainnet: addr1... " +
                       "- Testnet (preprod/preview): addr_test1... " +
                       "Perfect for: " +
                       "- Converting script hashes from script-usage-stats to queryable addresses " +
                       "- Finding UTXOs locked at a script address " +
                       "- Querying contract TVL using the address " +
                       "- Linking script analytics to on-chain data")
    public String scriptHashToAddress(
        @ToolParam(description = "Script hash in hex format (from script-usage-stats or other tools)")
        String scriptHash,

        @ToolParam(description = "Network flag: true for mainnet, false for testnet (preprod/preview)")
        boolean isMainnet
    ) {
        log.debug("Converting script hash to address: {}, isMainnet={}", scriptHash, isMainnet);

        if (scriptHash == null || scriptHash.trim().isEmpty()) {
            log.warn("Input script hash is null or empty");
            return null;
        }

        try {
            var credential = Credential.fromScript(scriptHash);
            String address = AddressProvider.getEntAddress(credential, isMainnet? Networks.mainnet(): Networks.testnet())
                            .toBech32();

            log.warn("script-hash-to-address not yet implemented - awaiting user implementation");
            return address;

        } catch (Exception e) {
            log.error("Failed to convert script hash to address: {}", scriptHash, e);
            return null;
        }
    }
}
