package com.bloxbean.cardano.yaci.store.common.genesis;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.crypto.Base58;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.CRC32;

@Slf4j
public class AvvmAddressConverter {

    //cddl: https://raw.githubusercontent.com/cardano-foundation/CIPs/master/CIP-0019/CIP-0019-byron-addresses.cddl
    public static Optional<String> convertAvvmToByronAddress(String avvmAddress) {
        try {
            String avvm = avvmAddress.replace("-", "+").replace("_", "/");

            //Spending data
            byte[] ed25519PubKey = Base64.getDecoder().decode(avvm);
            Array byronAddrSpendingDataArr = new Array();
            byronAddrSpendingDataArr.add(new UnsignedInteger(2));
            byronAddrSpendingDataArr.add(new ByteString(ed25519PubKey));

            //Address root
            Array addrRoot = new Array();
            int byronAddrType = 2;
            addrRoot.add(new UnsignedInteger(byronAddrType));
            addrRoot.add(byronAddrSpendingDataArr);
            Map byronAddrAttributes = new Map();
            addrRoot.add(byronAddrAttributes);

            byte[] addrRootBytes = CborSerializationUtil.serialize(addrRoot);

            //double hash
            byte[] sha3256Bytes = sha3256Bytes(addrRootBytes);
            byte[] byronAddrRootDoubleHash = Blake2bUtil.blake2bHash224(sha3256Bytes);

            //Byron payload
            Array byronAddrPayload = new Array();
            byronAddrPayload.add(new ByteString(byronAddrRootDoubleHash));
            byronAddrPayload.add(new Map());
            byronAddrPayload.add(new UnsignedInteger(2)); //Redeemption type

            //Address
            byte[] addressPayloadCbor = CborSerializationUtil.serialize(byronAddrPayload);
            CRC32 crc32 = new CRC32();
            crc32.update(addressPayloadCbor);

            Array byronAddressArr = new Array();
            ByteString payloadBytes = new ByteString(addressPayloadCbor);
            payloadBytes.setTag(24);
            byronAddressArr.add(payloadBytes);
            byronAddressArr.add(new UnsignedInteger(crc32.getValue()));

            byte[] byronAddressCbor = CborSerializationUtil.serialize(byronAddressArr);

            String base58Address = Base58.encode(byronAddressCbor);
            return Optional.of(base58Address);
        } catch (Exception e) {
            log.error("Error converting avvm address to Byron address", e);
            return Optional.empty();
        }
    }

    private static byte[] sha3256Bytes(byte[] addrRootBytes) throws NoSuchAlgorithmException {
        MessageDigest crypt = MessageDigest.getInstance("SHA3-256");
        crypt.update(addrRootBytes);
        byte[] sha3256Bytes = crypt.digest();
        return sha3256Bytes;
    }
}
