package com.bloxbean.cardano.yaci.store.common.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginDef {
    private String name;
    private String type;
    private String expression; //nullable
    private String inlineScript; //nullable
    private Script script; //nullable
    private boolean exitOnError = false;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Script {
        private String file;
        private String function; //nullable
    }
}
