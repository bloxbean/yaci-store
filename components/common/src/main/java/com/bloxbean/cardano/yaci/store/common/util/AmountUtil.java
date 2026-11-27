package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.model.Amount;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

/**
 * The ledger drops native assets with quantity 0 when it decodes a pre-Conway value or mint field
 * (Conway rejects them), so such entries are not part of the on-chain value.
 */
public final class AmountUtil {
    private AmountUtil() {
    }

    public static boolean isZeroQuantityAsset(Amount amount) {
        return amount != null
                && !LOVELACE.equals(amount.getUnit())
                && BigInteger.ZERO.equals(amount.getQuantity());
    }

    /** Returns the amounts without native assets of quantity 0. Lovelace is always kept. */
    public static List<Amount> withoutZeroQuantityAssets(List<Amount> amounts) {
        if (amounts == null)
            return Collections.emptyList();
        return amounts.stream()
                .filter(amount -> !isZeroQuantityAsset(amount))
                .toList();
    }
}
