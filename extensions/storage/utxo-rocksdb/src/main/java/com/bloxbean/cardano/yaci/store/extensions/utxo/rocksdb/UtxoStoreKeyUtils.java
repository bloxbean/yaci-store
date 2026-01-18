package com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb;

import com.bloxbean.cardano.client.address.util.AddressUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.rocks.types.common.KeyBuilder;

import java.nio.charset.StandardCharsets;

public class UtxoStoreKeyUtils {

    public static byte[] getKey(String txHash, int outputIndex) {
        byte[] txHashBytes = HexUtil.decodeHexString(txHash);
        byte[] outputIndexBytes = KeyBuilder.intToBytes(outputIndex);

        return new KeyBuilder(txHashBytes)
                .append(outputIndexBytes)
                .build();
    }

    public static byte[] getAddressBytes(String address) {
        try {
            return AddressUtil.addressToBytes(address);
        } catch (Exception e) {
            return address.getBytes(StandardCharsets.UTF_8);
        }
    }

    public static byte[] getPaymentCredential(String paymentCredential) {
        if (paymentCredential == null)
            return null;

        return HexUtil.decodeHexString(paymentCredential);
    }
}
