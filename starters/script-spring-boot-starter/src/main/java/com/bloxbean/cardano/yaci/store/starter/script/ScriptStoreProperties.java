package com.bloxbean.cardano.yaci.store.starter.script;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class ScriptStoreProperties {
    private Script script = new Script();

    @Getter
    @Setter
    public static final class Script  {
       private boolean enabled = true;
       private boolean apiEnabled = true;
       private Endpoints endpoints = new Endpoints();
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint script = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }
}
