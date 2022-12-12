package com.bloxbean.cardano.yaci.store.script.model;

import lombok.*;

import javax.persistence.Column;
import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class TxScriptId implements Serializable {
    @Column(name = "tx_hash")
    private String txHash;
    @Column(name = "script_hash")
    private String scriptHash;
}
