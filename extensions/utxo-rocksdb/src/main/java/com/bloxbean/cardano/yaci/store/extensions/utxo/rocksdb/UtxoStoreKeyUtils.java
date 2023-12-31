package com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb;

import com.bloxbean.cardano.client.address.util.AddressUtil;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.rocks.types.common.IndexRecord;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

public class UtxoStoreKeyUtils {

    public static String createHash(String first, String second) {
        String combined = first + "#" + second;
        return HexUtil.encodeHexString(Blake2bUtil.blake2bHash224(combined.getBytes(StandardCharsets.UTF_8)));
    }

    public static String getKey(String txHash, int outputIndex) {
        return txHash + "#" + outputIndex;
    }

    public static IndexRecord getKeyForPaymentCred(AddressUtxo addressUtxo) {
        IndexRecord indexRecord = new IndexRecord<>();
        indexRecord.setPartKey(addressUtxo.getOwnerPaymentCredential());
        indexRecord.setSecondaryKey(addressUtxo.getBlockNumber()
                + "#" + addressUtxo.getTxHash() + "#" + addressUtxo.getOutputIndex());

        return indexRecord;
    }

    public static List<IndexRecord> getKeyForPaymentCredAndAsset(AddressUtxo addressUtxo) {

        final String paymentCredential;

        if (addressUtxo.getOwnerPaymentCredential() != null)
            paymentCredential = addressUtxo.getOwnerPaymentCredential();
        else
            paymentCredential = addressUtxo.getOwnerAddr();

        return addressUtxo.getAmounts()
                .stream()
                .filter(amount -> !amount.getUnit().equals(LOVELACE))
                .map(amount -> {
                    String key = createHash(paymentCredential, amount.getUnit());
                    IndexRecord<Void> indexRecord = new IndexRecord<>();
                    String secondaryKey = addressUtxo.getBlockNumber()
                            + "#" + addressUtxo.getTxHash() + "#" + addressUtxo.getOutputIndex();

                    return new IndexRecord(key, secondaryKey);
                })
                .toList();
    }

    public static UtxoKey fromPaymentCredKey(String key) {
        String[] parts = key.split("#");
        if(parts.length != 3)
            throw new IllegalArgumentException("Invalid key");

        return new UtxoKey(parts[1], Integer.parseInt(parts[2]));
    }

    public static UtxoKey fromPaymentCredAndAssetKey(String key) {
        String[] parts = key.split("#");
        if(parts.length != 3)
            throw new IllegalArgumentException("Invalid key");

        return new UtxoKey(parts[1], Integer.parseInt(parts[2]));
    }

    public static List<IndexRecord> getSlotKey(AddressUtxo addressUtxo) {
        return List.of(new IndexRecord(String.valueOf(addressUtxo.getSlot()), addressUtxo.getTxHash() + "#" + addressUtxo.getOutputIndex()));
    }

    public static UtxoKey fromSlotKey(String key) {
        String[] parts = key.split("#");
        if(parts.length != 2)
            throw new IllegalArgumentException("Invalid key");

        return new UtxoKey(parts[0], Integer.parseInt(parts[1]));
    }

    public static List<IndexRecord> getTxInputSlotKey(TxInput txInput) {
        return List.of(new IndexRecord(String.valueOf(txInput.getSpentAtSlot()), txInput.getTxHash() + "#" + txInput.getOutputIndex()));
    }

    public static UtxoKey fromTxInputSlotKey(String key) {
        String[] parts = key.split("#");
        if(parts.length != 2)
            throw new IllegalArgumentException("Invalid key");

        return new UtxoKey(parts[0], Integer.parseInt(parts[1]));
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
