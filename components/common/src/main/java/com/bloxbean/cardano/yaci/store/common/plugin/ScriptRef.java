package com.bloxbean.cardano.yaci.store.common.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScriptRef {
    private String id;
    private String lang;
    private String file;
    private boolean enablePool;
}
