package com.bloxbean.cardano.yaci.store.plugin.api.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginDef {
    private String name;
    private String lang;
    private String expression; //nullable
    private String inlineScript; //nullable
    private ScriptDef script; //nullable
    private boolean exitOnError = false;
}
