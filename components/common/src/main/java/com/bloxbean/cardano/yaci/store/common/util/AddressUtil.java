package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.store.common.util.Tuple;

public class AddressUtil {
    private static final int MAX_ADDR_SIZE = 500; //Required for Byron Addresses

    //Return the address and full address if the address is too long
    //Using Tuple as Pair doesn't allow null values
    public static Tuple<String, String> getAddress(String address) {
        if (address != null && address.length() > MAX_ADDR_SIZE) {
            String addr = address.substring(0, MAX_ADDR_SIZE);
            String fullAddr = address;
            return new Tuple<>(addr, fullAddr);
        } else {
            return new Tuple<>(address, null);
        }
    }
}
