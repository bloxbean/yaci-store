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
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.mcp.server.model.BlockchainTimeInfo;
import com.bloxbean.cardano.yaci.store.mcp.server.model.CardanoNetworkInfo;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ConversionResult;
import com.bloxbean.cardano.yaci.store.mcp.server.model.SlotTimeInfo;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TimestampFormatted;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TimestampSlotInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP utility service for Cardano data conversions and unit conventions.
 * Provides tools to convert CBOR to JSON for metadata and datums, explains Cardano amount units,
 * and handles timestamp/slot conversions.
 * Essential for developers debugging and analyzing on-chain data.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * ⚠️ CRITICAL TIMESTAMP HANDLING RULES FOR LLMs:
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 1. ALL block_time VALUES = Unix timestamp in SECONDS (NOT milliseconds!)
 *    - To convert to milliseconds: block_time * 1000
 *    - This applies to ALL timestamps returned by Yaci Store MCP tools
 *
 * 2. SLOT TO TIMESTAMP CONVERSION:
 *    - For slot-only data: Use 'cardano-slot-to-timestamp' tool
 *    - Requires era information (BYRON or SHELLEY)
 *
 * 3. ERA RULES:
 *    - Byron blocks: Use Era.BYRON
 *    - All post-Byron (Shelley/Allegra/Mary/Alonzo/Babbage/Conway): Use Era.SHELLEY
 *    - When in doubt, use SHELLEY for modern blocks
 *
 * 4. TIMEZONE DISPLAY:
 *    - ALWAYS display times in user's local timezone
 *    - Infer timezone from user context (e.g., "Singapore time" → "Asia/Singapore")
 *    - If unknown, ask user or default to UTC
 *
 * 5. RECOMMENDED TOOLS:
 *    - Use 'cardano-blockchain-time-info' for comprehensive one-stop conversion
 *    - Use 'cardano-format-timestamp' for simple timestamp → local time
 *    - Use 'cardano-slot-to-timestamp' for slot → timestamp
 *    - Batch tool available for up to 50 slots: 'cardano-slots-to-timestamps-batch'
 *
 * 6. COMMON MISTAKES TO AVOID:
 *    - ❌ Don't treat block_time as milliseconds (it's seconds!)
 *    - ❌ Don't display UTC when user asks for local time
 *    - ❌ Don't add/subtract hours manually - use timezone tools
 *    - ❌ Don't exceed 50 slots in batch conversions
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpCardanoUtilService {
    private final GenesisConfig genesisConfig;
    private final StoreProperties storeProperties;
    private final EraService eraService;

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

    @Tool(name = "cardano-slot-to-timestamp",
          description = "⏰ Convert Cardano slot number to Unix timestamp. " +
                       "IMPORTANT: Returns timestamp in SECONDS (multiply by 1000 for milliseconds). " +
                       "Era handling: Use 'BYRON' for Byron blocks, 'SHELLEY' for all post-Byron blocks (Shelley/Allegra/Mary/Alonzo/Babbage/Conway). " +
                       "Automatically calculates block time using network genesis configuration and era-specific slot duration. " +
                       "Returns both seconds and milliseconds format plus ISO 8601 UTC time.")
    public SlotTimeInfo convertSlotToTimestamp(
        @ToolParam(description = "Slot number to convert") Long slot,
        @ToolParam(description = "Era: 'BYRON' or 'SHELLEY' (default: SHELLEY for modern blocks)") String era
    ) {
        log.debug("Converting slot {} to timestamp, era={}", slot, era);

        if (slot == null || slot < 0) {
            throw new IllegalArgumentException("Slot must be a non-negative number. Received: " + slot);
        }

        // Default to SHELLEY if not specified
        String effectiveEra = (era != null && era.trim().equalsIgnoreCase("BYRON")) ? "BYRON" : "SHELLEY";
        Era eraEnum = effectiveEra.equals("BYRON") ? Era.Byron : Era.Shelley;

        // Use EraService to get block time (in seconds)
        long timestampSeconds = eraService.blockTime(eraEnum, slot);
        long timestampMillis = timestampSeconds * 1000;

        // Format as ISO 8601 UTC
        Instant instant = Instant.ofEpochSecond(timestampSeconds);
        String utcTime = instant.toString();

        log.debug("Converted slot {} to timestamp: {} seconds ({} ms), UTC: {}",
                  slot, timestampSeconds, timestampMillis, utcTime);

        return new SlotTimeInfo(slot, timestampSeconds, timestampMillis, utcTime, effectiveEra);
    }

    @Tool(name = "cardano-format-timestamp",
          description = "⏰ Convert Unix timestamp (in SECONDS) to human-readable local time. " +
                       "CRITICAL: Input must be in SECONDS (Yaci Store block_time format), not milliseconds! " +
                       "Supports timezone IDs like 'Asia/Singapore', 'America/New_York', 'Europe/London', 'UTC'. " +
                       "Returns formatted time with timezone info, perfect for displaying blockchain times to users. " +
                       "Defaults to UTC if no timezone specified.")
    public TimestampFormatted formatTimestamp(
        @ToolParam(description = "Unix timestamp in SECONDS (block_time from Yaci Store)") Long timestampSeconds,
        @ToolParam(description = "Timezone ID (e.g., 'Asia/Singapore', 'UTC') - defaults to UTC") String timezone
    ) {
        log.debug("Formatting timestamp {} seconds with timezone {}", timestampSeconds, timezone);

        if (timestampSeconds == null || timestampSeconds < 0) {
            throw new IllegalArgumentException("Timestamp must be a non-negative number in seconds. Received: " + timestampSeconds);
        }

        // Default to UTC if not specified
        String effectiveTimezone = (timezone != null && !timezone.trim().isEmpty()) ? timezone : "UTC";

        try {
            ZoneId zoneId = ZoneId.of(effectiveTimezone);
            long timestampMillis = timestampSeconds * 1000;

            Instant instant = Instant.ofEpochSecond(timestampSeconds);
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId);

            // Format: "October 29, 2025 at 11:08:26 AM SGT"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm:ss a z");
            String formattedTime = zonedDateTime.format(formatter);

            // Also provide UTC for reference
            String utcTime = instant.toString();

            log.debug("Formatted timestamp: {} → {}", timestampSeconds, formattedTime);

            return new TimestampFormatted(timestampSeconds, timestampMillis, formattedTime, effectiveTimezone, utcTime);

        } catch (Exception e) {
            log.error("Failed to format timestamp with timezone {}: {}", effectiveTimezone, e.getMessage());
            throw new IllegalArgumentException("Invalid timezone: " + effectiveTimezone + ". Use format like 'Asia/Singapore' or 'UTC'");
        }
    }

    @Tool(name = "cardano-timestamp-to-slot",
          description = "⏰ Convert Unix timestamp to Cardano slot number with automatic era detection. " +
                       "Accepts timestamps in SECONDS or MILLISECONDS (auto-detects format). " +
                       "Returns slot number, detected era, and validation information. " +
                       "CRITICAL for LLMs: Use this to find slot numbers when you have dates/times and need to query blockchain data. " +
                       "Example: 'last 24 hours' → convert current time to slot, then subtract 86400 slots (Shelley). " +
                       "Validates conversion accuracy by round-trip check.")
    public TimestampSlotInfo convertTimestampToSlot(
        @ToolParam(description = "Unix timestamp (in seconds or milliseconds - will auto-detect)") Long timestamp,
        @ToolParam(description = "Force interpretation: true=seconds, false=milliseconds, null=auto-detect (default: null)") Boolean isSeconds
    ) {
        log.debug("Converting timestamp to slot: {}, isSeconds={}", timestamp, isSeconds);

        if (timestamp == null || timestamp <= 0) {
            throw new IllegalArgumentException("Timestamp must be a positive number");
        }

        // Step 1: Auto-detect seconds vs milliseconds
        Long timestampSeconds;
        String validationNote;

        if (isSeconds == null) {
            // Auto-detect: if > 10 billion, assume milliseconds (corresponds to year 2286)
            if (timestamp > 10_000_000_000L) {
                timestampSeconds = timestamp / 1000;
                validationNote = "Auto-detected milliseconds (timestamp > 10 billion). Converted to seconds: " + timestampSeconds;
                log.debug("Auto-detected milliseconds: {} → {} seconds", timestamp, timestampSeconds);
            } else {
                timestampSeconds = timestamp;
                validationNote = "Auto-detected seconds (timestamp <= 10 billion)";
                log.debug("Auto-detected seconds: {}", timestampSeconds);
            }
        } else if (isSeconds) {
            timestampSeconds = timestamp;
            validationNote = "User specified seconds";
        } else {
            timestampSeconds = timestamp / 1000;
            validationNote = "User specified milliseconds. Converted to seconds: " + timestampSeconds;
        }

        // Step 2: Call eraService.slotFromTime() to get slot
        Long calculatedSlot = eraService.slotFromTime(timestampSeconds);
        log.debug("Calculated slot: {} for timestamp: {}", calculatedSlot, timestampSeconds);

        // Step 3: Detect era by comparing timestamp with Shelley start time
        Long shelleyStartTime = eraService.shelleyEraStartTime();
        String detectedEra = timestampSeconds < shelleyStartTime ? "BYRON" : "SHELLEY";
        Era eraEnum = detectedEra.equals("BYRON") ? Era.Byron : Era.Shelley;
        log.debug("Detected era: {} (shelleyStartTime: {}, timestamp: {})",
                  detectedEra, shelleyStartTime, timestampSeconds);

        // Step 4: Validate by round-trip conversion
        Long validatedTimestamp = eraService.blockTime(eraEnum, calculatedSlot);
        long timeDifference = Math.abs(validatedTimestamp - timestampSeconds);

        if (timeDifference > 1) {
            log.warn("Round-trip validation failed: original={}, validated={}, difference={}s",
                    timestampSeconds, validatedTimestamp, timeDifference);
            validationNote += " | WARNING: Round-trip difference of " + timeDifference + " seconds. " +
                            "Timestamp may be between slots (slots are discrete: 1s for Shelley, 20s for Byron).";
        } else {
            validationNote += " | Validated: round-trip check passed (difference: " + timeDifference + "s)";
        }

        log.debug("Timestamp to slot conversion complete: {} → slot {}", timestampSeconds, calculatedSlot);

        return TimestampSlotInfo.create(
            timestamp,
            timestampSeconds,
            calculatedSlot,
            detectedEra,
            validationNote
        );
    }

    @Tool(name = "cardano-blockchain-time-info",
          description = "⏰ COMPREHENSIVE time converter - handles both slots and block_time values. " +
                       "⚠️ USE THIS FIRST when displaying blockchain times to users! " +
                       "Accepts either slot number OR block_time timestamp (in SECONDS). " +
                       "Automatically detects era and applies correct conversions. " +
                       "Returns complete time breakdown: UTC + formatted local time + all formats + helpful notes. " +
                       "Perfect one-stop tool for any blockchain time display needs. " +
                       "IMPORTANT: When providing blockTime only, do NOT provide slot parameter at all (not even 0 or null).")
    public BlockchainTimeInfo getBlockchainTimeInfo(
        @ToolParam(description = "Slot number (OPTIONAL - omit entirely if using blockTime)") Long slot,
        @ToolParam(description = "Block time in SECONDS (OPTIONAL - omit entirely if using slot)") Long blockTime,
        @ToolParam(description = "Era: 'BYRON' or 'SHELLEY' (required if using slot, ignored for blockTime)") String era,
        @ToolParam(description = "Timezone ID (e.g., 'Asia/Singapore', 'UTC') - defaults to UTC") String timezone
    ) {
        log.debug("Getting blockchain time info: slot={}, blockTime={}, era={}, timezone={}",
                  slot, blockTime, era, timezone);

        // Normalize: treat 0 as null for slot (since slot 0 is genesis and rarely used)
        Long normalizedSlot = (slot != null && slot == 0) ? null : slot;
        Long normalizedBlockTime = (blockTime != null && blockTime == 0) ? null : blockTime;

        // Validate inputs
        if (normalizedSlot == null && normalizedBlockTime == null) {
            throw new IllegalArgumentException("Either slot or blockTime must be provided");
        }

        // If both provided, prioritize blockTime (more direct timestamp value)
        if (normalizedSlot != null && normalizedBlockTime != null) {
            log.debug("Both slot ({}) and blockTime ({}) provided - using blockTime (more direct timestamp)",
                    normalizedSlot, normalizedBlockTime);
            normalizedSlot = null; // Ignore slot when both are present
        }

        // If slot provided, convert to block_time first
        Long effectiveBlockTime = normalizedBlockTime;
        String effectiveEra;

        if (normalizedSlot != null) {
            String slotEra = (era != null && era.trim().equalsIgnoreCase("BYRON")) ? "BYRON" : "SHELLEY";
            Era eraEnum = slotEra.equals("BYRON") ? Era.Byron : Era.Shelley;
            effectiveBlockTime = eraService.blockTime(eraEnum, normalizedSlot);
            effectiveEra = slotEra;
            log.debug("Converted slot {} to block_time: {} seconds (era: {})", normalizedSlot, effectiveBlockTime, effectiveEra);
        } else {
            // For blockTime only, we don't know the exact era, but for modern blocks it's SHELLEY
            effectiveEra = "SHELLEY (assumed for modern blocks)";
        }

        // Format the timestamp
        String effectiveTimezone = (timezone != null && !timezone.trim().isEmpty()) ? timezone : "UTC";

        try {
            ZoneId zoneId = ZoneId.of(effectiveTimezone);
            Instant instant = Instant.ofEpochSecond(effectiveBlockTime);
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId);

            // Format: "October 29, 2025 at 11:08:26 AM SGT"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm:ss a z");
            String localTime = zonedDateTime.format(formatter);

            // UTC format
            String utcTime = instant.toString();

            log.debug("Blockchain time info created: {} seconds → {} ({})", effectiveBlockTime, localTime, effectiveTimezone);

            return BlockchainTimeInfo.create(slot, effectiveBlockTime, utcTime, localTime, effectiveTimezone, effectiveEra);

        } catch (Exception e) {
            log.error("Failed to get blockchain time info: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid timezone: " + effectiveTimezone + ". Use format like 'Asia/Singapore' or 'UTC'");
        }
    }

    @Tool(name = "cardano-slots-to-timestamps-batch",
          description = "⏰ Convert multiple slot numbers to timestamps in one call (MAX 50 slots). " +
                       "⚠️ DoS Protection: Limited to 50 slots per batch to prevent resource exhaustion. " +
                       "Efficiently converts a list of slots to timestamps with full time information. " +
                       "Each result includes seconds, milliseconds, and ISO 8601 UTC time. " +
                       "Use this for analyzing multiple blocks or time points efficiently. " +
                       "Era handling: Use 'BYRON' for Byron blocks, 'SHELLEY' for all post-Byron blocks.")
    public List<SlotTimeInfo> convertSlotsBatch(
        @ToolParam(description = "List of slot numbers to convert (max 50 slots)") List<Long> slots,
        @ToolParam(description = "Era: 'BYRON' or 'SHELLEY' (default: SHELLEY)") String era
    ) {
        log.debug("Batch converting {} slots to timestamps, era={}", slots != null ? slots.size() : 0, era);

        // Validate inputs
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Slots list cannot be null or empty");
        }
        if (slots.size() > 50) {
            throw new IllegalArgumentException(
                "Batch size limited to 50 slots to prevent resource exhaustion. Received: " + slots.size() +
                ". Please split into smaller batches or use single conversion tool.");
        }

        // Default to SHELLEY if not specified
        String effectiveEra = (era != null && era.trim().equalsIgnoreCase("BYRON")) ? "BYRON" : "SHELLEY";
        Era eraEnum = effectiveEra.equals("BYRON") ? Era.Byron : Era.Shelley;

        List<SlotTimeInfo> results = new ArrayList<>();

        for (Long slot : slots) {
            if (slot == null || slot < 0) {
                log.warn("Skipping invalid slot: {}", slot);
                continue;
            }

            try {
                // Use EraService to get block time (in seconds)
                long timestampSeconds = eraService.blockTime(eraEnum, slot);
                long timestampMillis = timestampSeconds * 1000;

                // Format as ISO 8601 UTC
                Instant instant = Instant.ofEpochSecond(timestampSeconds);
                String utcTime = instant.toString();

                results.add(new SlotTimeInfo(slot, timestampSeconds, timestampMillis, utcTime, effectiveEra));

            } catch (Exception e) {
                log.error("Failed to convert slot {} to timestamp: {}", slot, e.getMessage());
                // Continue processing other slots
            }
        }

        log.debug("Batch converted {} slots successfully", results.size());
        return results;
    }
}
