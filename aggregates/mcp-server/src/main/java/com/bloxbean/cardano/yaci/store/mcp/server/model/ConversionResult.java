package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing the result of a CBOR to JSON conversion.
 * Used by utility conversion tools.
 */
public record ConversionResult(
    String inputCbor,      // Original CBOR hex input
    String outputJson,     // Converted JSON output (null if conversion failed)
    String format,         // Format type: "metadata" or "datum"
    boolean success,       // Whether conversion succeeded
    String error           // Error message if conversion failed (null if success)
) {
    /**
     * Create a successful conversion result.
     */
    public static ConversionResult success(String inputCbor, String outputJson, String format) {
        return new ConversionResult(inputCbor, outputJson, format, true, null);
    }

    /**
     * Create a failed conversion result.
     */
    public static ConversionResult failure(String inputCbor, String format, String error) {
        return new ConversionResult(inputCbor, null, format, false, error);
    }
}
