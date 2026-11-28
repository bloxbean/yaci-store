package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;

import java.math.BigInteger;
import java.util.List;

/**
 * Registry node outputs of the mainnet CIP-113 registry, as yaci-store's utxo store indexed them (unspent on 2026-11-24).
 */
public final class Cip113MainnetNodes {

    public static final String REGISTRY_POLICY_ID = "484e733d122af44e6101988bcc47ed261a7af5c43c797e89d60e3075";

    /** Origin node: empty key, every credential an empty verification key, no global state. */
    public static final String ORIGIN_DATUM =
            "d8799f40581c01c24df7941f8b5856762fcc8aa0bb61a8c24f0911ed6aef474034d0d8799f40ffd8799f40ffd8799f40ffd8799f40ff40ff";

    public static final String POLICY_01C2 = "01c24df7941f8b5856762fcc8aa0bb61a8c24f0911ed6aef474034d0";
    /** Policy 01c24df7…: script credentials, no unfracking script, a global state. Tx 8ee8f76e…#2, slot 199,716,706. */
    public static final String POLICY_01C2_DATUM =
            "d8799f581c01c24df7941f8b5856762fcc8aa0bb61a8c24f0911ed6aef474034d0581cb025efe5b44b43ed154419c66b0efc9cf148fd98f464e261018c89e8d87a9f581c4e9ba4785eebbeb77fe694c8451f1abec411f7774c09bbacfa3ba1acffd87a9f581cddf0363f0d73f54778e792e16bad802df5fa50061319b1f01d280a37ffd87a9f581c446fc9584df8316cfab440c60d140e2007f236cdb65180917a374fb8ffd8799f40ff581c218ba0c5b7156b13c51e41096e34f0310823adf0e0d3fc70a741f3c2ff";

    public static final String POLICY_B6F4 = "b6f47e52441e75240712320fd152e7bbf8b7aa11c2c3b6f761e0bbe4";
    /** Policy b6f47e52…: no global state. Tx caf636cb…#1, slot 203,648,950. */
    public static final String POLICY_B6F4_DATUM =
            "d8799f581cb6f47e52441e75240712320fd152e7bbf8b7aa11c2c3b6f761e0bbe4581ced5f6bb66712a157f0219978e8a17c5b4f9a7cb59d54714bd5984344d87a9f581cc34be15de6ba21909a5a600d61b36056064fe90594bdbd511ff117ceffd87a9f581c87cb2e1bd2d85450a7574c4ab243a28f2bf4eaabe19fd58b16d855aeffd87a9f581c3c343901382560cdd602f87f90eeb62d0b6ad97d9f0ca182ae86ff53ffd8799f40ff40ff";

    private Cip113MainnetNodes() {
    }

    /** An output at the registry's script address holding the registry NFT {@code key} and the datum. */
    public static AddressUtxo nodeOutput(String txHash, int txIndex, int outputIndex, String key, String datum) {
        return output(txHash, txIndex, outputIndex, REGISTRY_POLICY_ID, REGISTRY_POLICY_ID + key, BigInteger.ONE, datum);
    }

    public static AddressUtxo output(String txHash, int txIndex, int outputIndex, String paymentCredential, String unit,
                                     BigInteger quantity, String datum) {
        return AddressUtxo.builder()
                .txHash(txHash)
                .txIndex(txIndex)
                .outputIndex(outputIndex)
                .ownerPaymentCredential(paymentCredential)
                .amounts(List.of(
                        Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(2_000_000)).build(),
                        Amt.builder().unit(unit).policyId(unit.substring(0, 56)).assetName(unit.substring(56))
                                .quantity(quantity).build()))
                .inlineDatum(datum)
                .build();
    }
}
