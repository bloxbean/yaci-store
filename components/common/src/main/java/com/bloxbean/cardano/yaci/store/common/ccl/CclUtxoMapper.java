package com.bloxbean.cardano.yaci.store.common.ccl;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for converting Yaci Store UTxO domain objects to Cardano
 * Client Lib {@link Utxo} objects.
 * <p>.
 */
public final class CclUtxoMapper {
    private CclUtxoMapper() {
    }

    /**
     * Converts an address UTxO returned by the local UTxO client to CCL's
     * {@link Utxo} model.
     *
     * @param addressUtxo Yaci Store address UTxO
     * @return the equivalent CCL UTxO, or {@code null} when the input is {@code null}
     */
    public static Utxo fromAddressUtxo(AddressUtxo addressUtxo) {
        if (addressUtxo == null)
            return null;

        return Utxo.builder()
                .txHash(addressUtxo.getTxHash())
                .outputIndex(addressUtxo.getOutputIndex() != null ? addressUtxo.getOutputIndex() : 0)
                .address(addressUtxo.getOwnerAddr())
                .amount(fromAmounts(addressUtxo.getLovelaceAmount(), addressUtxo.getAmounts()))
                .dataHash(addressUtxo.getDataHash())
                .inlineDatum(addressUtxo.getInlineDatum())
                .referenceScriptHash(addressUtxo.getReferenceScriptHash())
                .build();
    }

    /**
     * Converts the API-facing Yaci Store UTxO model to CCL's {@link Utxo}
     * model.
     *
     * @param utxo Yaci Store UTxO
     * @return the equivalent CCL UTxO, or {@code null} when the input is {@code null}
     */
    public static Utxo fromStoreUtxo(com.bloxbean.cardano.yaci.store.common.domain.Utxo utxo) {
        if (utxo == null)
            return null;

        return Utxo.builder()
                .txHash(utxo.getTxHash())
                .outputIndex(utxo.getOutputIndex())
                .address(utxo.getAddress())
                .amount(fromStoreAmounts(utxo.getAmount()))
                .dataHash(utxo.getDataHash())
                .inlineDatum(utxo.getInlineDatum())
                .referenceScriptHash(utxo.getReferenceScriptHash())
                .build();
    }

    private static List<Amount> fromAmounts(BigInteger lovelaceAmount, List<Amt> amounts) {
        List<Amount> result = new ArrayList<>();
        if (lovelaceAmount != null)
            result.add(Amount.lovelace(lovelaceAmount));

        if (amounts == null)
            return result;

        for (Amt amt : amounts) {
            if (amt == null)
                continue;

            String unit = amt.getUnit();
            if (unit == null && amt.getPolicyId() != null)
                unit = amt.getPolicyId() + (amt.getAssetName() != null ? amt.getAssetName() : "");

            if (unit != null && amt.getQuantity() != null)
                result.add(Amount.asset(unit, amt.getQuantity()));
        }

        return result;
    }

    private static List<Amount> fromStoreAmounts(List<com.bloxbean.cardano.yaci.store.common.domain.Utxo.Amount> amounts) {
        if (amounts == null)
            return Collections.emptyList();

        return amounts.stream()
                .map(amount -> new Amount(amount.getUnit(), amount.getQuantity()))
                .toList();
    }
}
