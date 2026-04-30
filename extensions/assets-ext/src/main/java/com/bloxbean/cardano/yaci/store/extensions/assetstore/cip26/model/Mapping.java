package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Parsed representation of a single CIP-26 token metadata mapping file from
 * <a href="https://github.com/cardano-foundation/cardano-token-registry">cardano-token-registry</a>.
 * Field names mirror the CIP-26 JSON schema exactly so Jackson can bind 1:1.
 *
 * @param subject     The asset identifier, base16-encoded. For phase-1 monetary policies this is
 *                    {@code policyId (28 bytes) || assetName (up to 32 bytes)}, so at most
 *                    120 hex chars. Example: LAMP token's subject is
 *                    {@code 2376e2c5…5b5c} (policyId) + {@code 4c414d50} ("LAMP").
 * @param policy      The base16-encoded CBOR of the phase-1 monetary script (a native script).
 *                    <strong>This is the script itself, NOT the 28-byte policyId hash.</strong>
 *                    Per CIP-26: {@code minLength: 56, maxLength: 120} hex chars. Clients verify
 *                    an entry by re-hashing this field with blake2b-224 and comparing the result
 *                    to the first 28 bytes of {@code subject}. A simple single-sig script is
 *                    ~64 hex chars; time-locked or multisig scripts can reach the 120 cap.
 * @see <a href="https://github.com/cardano-foundation/CIPs/blob/main/CIP-0026/README.md">CIP-0026</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Mapping(String subject,
                      Item url,
                      Item name,
                      Item ticker,
                      Item decimals,
                      Item logo,
                      String policy,
                      Item description) {
}
