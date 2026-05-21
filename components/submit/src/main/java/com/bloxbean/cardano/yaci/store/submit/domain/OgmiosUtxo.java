package com.bloxbean.cardano.yaci.store.submit.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parsed representation of an {@code additionalUtxoSet} entry accepted by the
 * transaction evaluation endpoint.
 * <p>
 * The parser accepts the Blockfrost documented {@code [TxIn, TxOut]} tuple
 * shape and the flat Ogmios UTxO object shape used by Ogmios schemas.
 */
@Getter
@NoArgsConstructor
public class OgmiosUtxo {
    private TxIn txIn;
    private TxOut txOut;

    /**
     * Creates an additional UTxO from a transaction input and output.
     *
     * @param txIn transaction input
     * @param txOut transaction output
     */
    public OgmiosUtxo(TxIn txIn, TxOut txOut) {
        this.txIn = txIn;
        this.txOut = txOut;
    }

    /**
     * Parses an {@code additionalUtxoSet} JSON array.
     *
     * @param node additional UTxO set JSON node
     * @return parsed additional UTxOs, or an empty list when the node is {@code null}
     * @throws IllegalArgumentException when the JSON shape is not supported
     */
    public static List<OgmiosUtxo> fromAdditionalUtxoSet(JsonNode node) {
        if (node == null || node.isNull())
            return Collections.emptyList();

        if (!(node instanceof ArrayNode arrayNode))
            throw new IllegalArgumentException("additionalUtxoSet must be an array");

        List<OgmiosUtxo> additionalUtxos = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            additionalUtxos.add(from(item));
        }

        return additionalUtxos;
    }

    /**
     * Parses a single additional UTxO entry.
     *
     * @param node tuple or flat UTxO JSON node
     * @return parsed additional UTxO
     * @throws IllegalArgumentException when required fields are missing
     */
    public static OgmiosUtxo from(JsonNode node) {
        if (node == null || node.isNull())
            throw new IllegalArgumentException("additionalUtxoSet entry is required");

        // Blockfrost documents additionalUtxoSet as [TxIn, TxOut] tuples.
        // Ogmios schemas have also exposed a flat UTxO object, so accept both
        // typed forms instead of falling back to unstructured request objects.
        if (node.isArray()) {
            if (node.size() != 2)
                throw new IllegalArgumentException("additionalUtxoSet entries must be [TxIn, TxOut] tuples");

            return new OgmiosUtxo(TxIn.from(node.get(0)), TxOut.from(node.get(1)));
        } else if (node.isObject()) {
            return new OgmiosUtxo(TxIn.from(node), TxOut.from(node));
        } else {
            throw new IllegalArgumentException("additionalUtxoSet entries must be objects or [TxIn, TxOut] tuples");
        }
    }

    /**
     * Transaction input portion of an additional UTxO entry.
     */
    @Getter
    @NoArgsConstructor
    public static class TxIn {
        private String txId;
        private Integer index;

        /**
         * Creates a transaction input reference.
         *
         * @param txId transaction id
         * @param index output index within the transaction
         */
        public TxIn(String txId, Integer index) {
            this.txId = txId;
            this.index = index;
        }

        private static TxIn from(JsonNode node) {
            String txId = text(node, "txId", "tx_id");
            if (txId == null)
                txId = text(child(node, "transaction"), "id");

            Integer index = intValue(node, "index", "outputIndex", "output_index");
            if (index == null)
                index = intValue(child(node, "output"), "index");

            if (isBlank(txId))
                throw new IllegalArgumentException("additionalUtxoSet TxIn transaction id is required");
            if (index == null)
                throw new IllegalArgumentException("additionalUtxoSet TxIn index is required");

            return new TxIn(txId, index);
        }
    }

    /**
     * Transaction output portion of an additional UTxO entry.
     */
    @Getter
    @NoArgsConstructor
    public static class TxOut {
        private String address;
        private JsonNode value;
        private String datumHash;
        private JsonNode datum;
        private JsonNode script;
        private String referenceScriptHash;

        /**
         * Creates a transaction output for an additional UTxO entry.
         *
         * @param address output address
         * @param value output value in Ogmios or Blockfrost-compatible JSON shape
         * @param datumHash datum hash, when present
         * @param datum inline datum, when present
         * @param script reference script JSON, when present
         * @param referenceScriptHash reference script hash, when present
         */
        public TxOut(String address, JsonNode value, String datumHash, JsonNode datum, JsonNode script, String referenceScriptHash) {
            this.address = address;
            this.value = value;
            this.datumHash = datumHash;
            this.datum = datum;
            this.script = script;
            this.referenceScriptHash = referenceScriptHash;
        }

        private static TxOut from(JsonNode node) {
            String address = text(node, "address", "ownerAddr", "owner_addr");
            JsonNode value = child(node, "value");
            if (isBlank(address))
                throw new IllegalArgumentException("additionalUtxoSet TxOut address is required");
            if (value == null)
                throw new IllegalArgumentException("additionalUtxoSet TxOut value is required");

            return new TxOut(
                    address,
                    value,
                    text(node, "datumHash", "datum_hash", "dataHash", "data_hash"),
                    child(node, "datum", "inlineDatum", "inline_datum"),
                    child(node, "script"),
                    text(node, "referenceScriptHash", "reference_script_hash")
            );
        }
    }

    private static JsonNode child(JsonNode node, String... names) {
        if (node == null || node.isNull())
            return null;

        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull())
                return value;
        }

        return null;
    }

    private static String text(JsonNode node, String... names) {
        JsonNode value = child(node, names);
        return value != null ? value.asText() : null;
    }

    private static Integer intValue(JsonNode node, String... names) {
        JsonNode value = child(node, names);
        return value != null ? value.asInt() : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
