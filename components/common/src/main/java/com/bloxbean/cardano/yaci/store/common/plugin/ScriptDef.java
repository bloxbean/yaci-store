package com.bloxbean.cardano.yaci.store.common.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScriptDef {
    private String id; //nullable
    private String file; //nullable
    private String function; //nullable
}
