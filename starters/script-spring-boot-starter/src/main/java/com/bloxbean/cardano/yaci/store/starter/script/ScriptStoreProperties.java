package com.bloxbean.cardano.yaci.store.starter.script;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class ScriptStoreProperties {
    private Script script;

    @Getter
    @Setter
    public static final class Script  {
       private boolean enabled = true;
    }

}
