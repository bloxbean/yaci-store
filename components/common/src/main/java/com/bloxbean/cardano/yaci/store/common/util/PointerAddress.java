package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.client.address.Pointer;
import com.bloxbean.cardano.client.exception.AddressRuntimeException;
import com.bloxbean.cardano.client.util.Tuple;

import java.util.Arrays;

//TODO -- Remove this class once the cardano-client-lib is updated to 0.5.2
/**
 * PointerAddress class represents Shelley Pointer address. This class is useful to decode Pointer address and get the Pointer
 */
public class PointerAddress extends Address {
    private Pointer pointer;

    public PointerAddress(String prefix, byte[] bytes) {
        super(prefix, bytes);

        if (getAddressType() != AddressType.Ptr)
            throw new AddressRuntimeException("Invalid address type. Expected Pointer address type");

        decodePointer();
    }

    public PointerAddress(String address) {
        super(address);

        if (getAddressType() != AddressType.Ptr)
            throw new AddressRuntimeException("Invalid address type. Expected Pointer address type");

        decodePointer();
    }

    public PointerAddress(byte[] addressBytes) {
        super(addressBytes);

        if (getAddressType() != AddressType.Ptr)
            throw new AddressRuntimeException("Invalid address type. Expected Pointer address type");

        decodePointer();
    }

    private void decodePointer() {
        byte[] pointerBytes = getDelegationCredentialHash()
                .orElseThrow(() -> new AddressRuntimeException("Delegation credential hash not found"));

        int index = 0;
        Tuple<Long, Integer> slot = variableNatDecode(pointerBytes);
        index += slot._2;
        Tuple<Long, Integer> txIndex = variableNatDecode(getSubBytes(pointerBytes, index));
        index += txIndex._2;
        Tuple<Long, Integer> certIndex = variableNatDecode(getSubBytes(pointerBytes, index));

        pointer = new Pointer(slot._1, txIndex._1.intValue(), certIndex._1.intValue());
    }

    private Tuple<Long, Integer> variableNatDecode(byte[] raw) {
        long output = 0;
        int bytesRead = 0;

        for (byte rbyte : raw) {
            output = (output << 7) | (rbyte & 0x7F);
            bytesRead++;

            if ((rbyte & 0x80) == 0) {
                return new Tuple<>(output, bytesRead);
            }
        }

        throw new IllegalArgumentException("Invalid variable nat encoding. Unexpected bytes");
    }

    // Get subarray from startPosition to the end of the array
    private byte[] getSubBytes(byte[] array, int startPosition) {
        // Validate startPosition
        if (startPosition < 0 || startPosition > array.length) {
            throw new IllegalArgumentException("Invalid start position");
        }

        return Arrays.copyOfRange(array, startPosition, array.length);
    }

    public Pointer getPointer() {
        return pointer;
    }
}

