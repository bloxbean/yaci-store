package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.cache.RocksDBCache;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;

import java.nio.ByteBuffer;

/** An output's key as 34 bytes in a {@link RocksDBCache}: the 32 bytes of its tx hash, then its index. */
public class UtxoKeyCodec implements RocksDBCache.Codec<UtxoKey> {
    public static final UtxoKeyCodec INSTANCE = new UtxoKeyCodec();

    @Override
    public byte[] encode(UtxoKey utxoKey) {
        byte[] txHash = HexUtil.decodeHexString(utxoKey.getTxHash());
        return ByteBuffer.allocate(txHash.length + 2).put(txHash).putShort(utxoKey.getOutputIndex().shortValue()).array();
    }

    @Override
    public UtxoKey decode(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte[] txHash = new byte[bytes.length - 2];
        buffer.get(txHash);
        return new UtxoKey(HexUtil.encodeHexString(txHash), Short.toUnsignedInt(buffer.getShort()));
    }
}
