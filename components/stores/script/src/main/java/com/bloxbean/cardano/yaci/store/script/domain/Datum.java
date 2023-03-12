package com.bloxbean.cardano.yaci.store.script.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Datum {
    private String hash;
    private String datum;
    private String createdAtTx;
}
