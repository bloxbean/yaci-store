package com.bloxbean.cardano.yaci.store.starter.cip139;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class Cip139Properties {
    private Extensions extensions = new Extensions();

    @Getter
    @Setter
    public static final class Extensions  {

        private Cip139 cip139 = new Cip139();

        @Getter
        @Setter
        public static final class Cip139  {

            private ProtocolParameters protocolParameters = new ProtocolParameters();

            @Getter
            @Setter
            public static final class ProtocolParameters {
                private boolean enabled = true;
            }

        }

    }

}
