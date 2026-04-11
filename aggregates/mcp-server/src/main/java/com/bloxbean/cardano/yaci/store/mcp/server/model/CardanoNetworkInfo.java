package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model containing Cardano network genesis configuration and timing parameters.
 * Essential for LLMs to correctly calculate slot/epoch conversions and time ranges.
 */
public record CardanoNetworkInfo(
    String networkType,              // e.g., "MAINNET", "PREPROD", "PREVIEW"
    long protocolMagic,              // Network identifier
    double shelleySlotLength,        // Shelley era slot length in seconds (typically 1.0)
    long byronSlotLength,            // Byron era slot length in seconds (typically 20)
    long epochLength,                // Slots per epoch (e.g., 432000 for mainnet = 5 days)
    long startTime,                  // Network start time (Unix timestamp)
    String conversionGuide           // Help text for slot/time calculations
) {
    /**
     * Create CardanoNetworkInfo with conversion guide.
     */
    public static CardanoNetworkInfo create(
        String networkType,
        long protocolMagic,
        double shelleySlotLength,
        long byronSlotLength,
        long epochLength,
        long startTime
    ) {
        String conversionGuide = String.format("""
            ══════════════════════════════════════════════════════════════════
            TIME & SLOT CONVERSION GUIDE
            ══════════════════════════════════════════════════════════════════

            SLOT DURATION (Shelley era): %.1f seconds
            EPOCH LENGTH: %,d slots = %.1f days

            ══════════════════════════════════════════════════════════════════
            CONVERSION FORMULAS:
            ══════════════════════════════════════════════════════════════════

            Time to Slots:
            - 1 second = 1/%.1f = %.2f slots
            - 1 minute = 60 slots
            - 1 hour = 3,600 slots
            - 1 day = 86,400 slots
            - 1 week = 604,800 slots

            Slots to Time:
            - 1 slot = %.1f second
            - 100 slots = %.1f minutes
            - 1,000 slots = %.1f minutes
            - 10,000 slots = %.1f hours

            Examples:
            - Last 24 hours = 86,400 slots
            - Last week = 604,800 slots
            - Last month (~30 days) = 2,592,000 slots
            - Last epoch = %,d slots = %.1f days

            ══════════════════════════════════════════════════════════════════
            GOLDEN RULES FOR SLOT CALCULATIONS:
            ══════════════════════════════════════════════════════════════════
            1. ALWAYS use %d seconds per slot (Shelley era)
            2. For time ranges, multiply duration in seconds by 1 to get slots
            3. Example: "last 24 hours" = 24 * 60 * 60 * 1 = 86,400 slots
            4. Block time is ~20 seconds (not 1 second!)
            5. Epoch length is %,d slots = %.1f days

            ══════════════════════════════════════════════════════════════════
            """,
            shelleySlotLength,
            epochLength, epochLength * shelleySlotLength / 86400.0,
            shelleySlotLength, 1.0 / shelleySlotLength,
            shelleySlotLength,
            100 * shelleySlotLength / 60.0,
            1000 * shelleySlotLength / 60.0,
            10000 * shelleySlotLength / 3600.0,
            epochLength, epochLength * shelleySlotLength / 86400.0,
            (int) shelleySlotLength,
            epochLength, epochLength * shelleySlotLength / 86400.0
        );

        return new CardanoNetworkInfo(
            networkType,
            protocolMagic,
            shelleySlotLength,
            byronSlotLength,
            epochLength,
            startTime,
            conversionGuide
        );
    }
}
