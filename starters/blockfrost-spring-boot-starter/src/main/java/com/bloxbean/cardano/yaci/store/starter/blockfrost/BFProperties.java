package com.bloxbean.cardano.yaci.store.starter.blockfrost;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class BFProperties {
    private Extensions extensions = new Extensions();

    @Getter
    @Setter
    public static final class Extensions  {

        private Blockfrost blockfrost = new Blockfrost();

        @Getter
        @Setter
        public static final class Blockfrost  {

            private Epoch epoch = new Epoch();

            @Getter
            @Setter
            public static final class Epoch {
                private boolean enabled = true;
            }

        }

    }

}
