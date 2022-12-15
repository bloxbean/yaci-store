package com.bloxbean.cardano.yaci.store.script.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PlutusScript extends Script {
    private String scriptHash;
    private String content;
    private String type;
}
