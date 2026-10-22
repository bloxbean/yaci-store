package com.bloxbean.cardano.yaci.store.blockfrost.transaction.util;

import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BFTxSizeUtilTest {

    @Test
    void givenAlonzoPlusFullTxCbor_shouldExcludeIsValidByte() throws IOException {
        // preview tx df39d52e...: 815 bytes on the wire, Blockfrost / db-sync tx.size = 814 (#1176)
        byte[] cbor = readHex("tx-cbor/preview-df39d52e.hex");

        assertThat(cbor).hasSize(815);
        assertThat(cbor[0]).isEqualTo((byte) 0x84);
        assertThat(BFTxSizeUtil.ledgerSize(cbor)).isEqualTo(814);
    }

    @Test
    void givenShelleyFullTxCbor_shouldReturnLength() throws IOException {
        // preprod tx a00696a0... (epoch 4, Shelley): db-sync tx.size = 1156
        byte[] cbor = readHex("tx-cbor/preprod-a00696a0-shelley.hex");

        assertThat(cbor[0]).isEqualTo((byte) 0x83);
        assertThat(BFTxSizeUtil.ledgerSize(cbor)).isEqualTo(1156);
    }

    @Test
    void givenBodyOnlyCbor_shouldReturnNull() {
        // store.transaction.save-cbor stores the body only: a map, not a full transaction
        byte[] body = HexUtil.decodeHexString("a300818258200000000000000000000000000000000000000000000000000000000000000000000180021a00029810");

        assertThat(BFTxSizeUtil.ledgerSize(body)).isNull();
    }

    @Test
    void givenNoCbor_shouldReturnNull() {
        assertThat(BFTxSizeUtil.ledgerSize(null)).isNull();
        assertThat(BFTxSizeUtil.ledgerSize(new byte[0])).isNull();
    }

    private byte[] readHex(String resource) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            return HexUtil.decodeHexString(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
        }
    }
}
