package com.bloxbean.cardano.yaci.store.common.util;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.client.exception.AddressRuntimeException;
import com.bloxbean.cardano.client.plutus.spec.Redeemer;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class Util {

    public static Optional<Redeemer> deserialize(String cbor) {
        try {
            DataItem di = CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(cbor));
            return Optional.of(Redeemer.deserialize((Array) di));
        } catch (Exception e) {
            log.error("Error deserializing redeemer : " + cbor);
            return Optional.empty();
        }
    }

    public static Optional<String> getPaymentKeyHash(Address address) {
        AddressType addressType = address.getAddressType();
        byte[] addressBytes = address.getBytes();

        byte[] paymentKeyHash;
        switch (addressType) {
            case Base:
            case Enterprise:
            case Ptr:
                paymentKeyHash = new byte[28];
                System.arraycopy(addressBytes, 1, paymentKeyHash, 0, paymentKeyHash.length);
                break;
            default: {
                log.error("Unsupported address type: " + addressType + ", address=" + address.toBech32());
                return Optional.empty();
            }
        }
        return Optional.of(HexUtil.encodeHexString(paymentKeyHash));
    }

    public static Optional<String> getStakeKeyHash(Address address) {
            AddressType addressType = address.getAddressType();
            byte[] addressBytes = address.getBytes();

            byte[] stakeKeyHash;
            switch (addressType) {
                case Base:
                    stakeKeyHash = new byte[28];
                    System.arraycopy(addressBytes, 1 + 28, stakeKeyHash, 0, stakeKeyHash.length);
                    break;
                case Enterprise:
                    stakeKeyHash = null;
                    break;
                case Reward:
                    stakeKeyHash = new byte[28];
                    System.arraycopy(addressBytes, 1, stakeKeyHash, 0, stakeKeyHash.length);
                    break;
                case Ptr:
                    stakeKeyHash = new byte[addressBytes.length - 1 - 28];
                    System.arraycopy(addressBytes, 1 + 28, stakeKeyHash, 0, stakeKeyHash.length);
                    break;
                default:
                    throw new AddressRuntimeException("StakeKeyHash can't be found for address type : " + addressType);
            }

            return stakeKeyHash != null?
                    Optional.of(HexUtil.encodeHexString(stakeKeyHash))
                    : Optional.empty();
    }
}
