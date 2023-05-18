package com.bloxbean.cardano.yaci.store.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@ToString
public class GenesisBalance {
    private String address;
    private String txnHash;
    private BigInteger balance;
}
