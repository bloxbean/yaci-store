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

            private boolean enabled = false;

            private Epoch epoch = new Epoch();
            private Address address = new Address();
            private Asset asset = new Asset();
            private Account account = new Account();
            private Transaction transaction = new Transaction();
            private Blocks blocks = new Blocks();
            private Scripts scripts = new Scripts();
            private Metadata metadata = new Metadata();
            private Util util = new Util();

            @Getter
            @Setter
            public static final class Epoch {
                private boolean enabled = true;
            }

            @Getter
            @Setter
            public static final class Address {
                private boolean enabled = true;
            }

            @Getter
            @Setter
            public static final class Asset {
                private boolean enabled = true;
            }

            @Getter
            @Setter
            public static final class Account {
                private boolean enabled = true;
            }

            @Getter
            @Setter
            public static final class Transaction {
                private boolean enabled = true;
            }

            @Getter
            @Setter
            public static final class Blocks {
                private boolean enabled = true;
            }

            @Getter
            @Setter
            public static final class Scripts {
                private boolean enabled = true;
            }

            @Getter
            @Setter
            public static final class Metadata {
                private boolean enabled = true;
            }

            @Getter
            @Setter
            public static final class Util {
                private boolean enabled = true;
            }

        }

    }

}
