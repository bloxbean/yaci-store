package com.bloxbean.cardano.yaci.store.utxo.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PtrAddress {
    private String address;
    private String stakeAddress;
}
