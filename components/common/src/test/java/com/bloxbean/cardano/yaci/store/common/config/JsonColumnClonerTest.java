package com.bloxbean.cardano.yaci.store.common.config;

import io.hypersistence.utils.hibernate.type.util.Configuration;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class JsonColumnClonerTest {
    private final JsonColumnCloner cloner = new JsonColumnCloner();

    static class NotSerializable {
        private String name;
        private BigInteger amount;

        public NotSerializable() {
        }

        NotSerializable(String name, BigInteger amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigInteger getAmount() { return amount; }
        public void setAmount(BigInteger amount) { this.amount = amount; }

        @Override
        public boolean equals(Object o) {
            return o instanceof NotSerializable other && Objects.equals(name, other.name)
                    && Objects.equals(amount, other.amount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, amount);
        }
    }

    @Test
    void copiesNonSerializableObjectsThroughJson() {
        NotSerializable value = new NotSerializable("vrf", BigInteger.TEN);

        NotSerializable copy = cloner.clone(value);

        assertThat(copy).isEqualTo(value).isNotSameAs(value);
    }

    @Test
    void copiesCollectionsAndMapsOfNonSerializableObjects() {
        List<NotSerializable> list = new ArrayList<>(List.of(new NotSerializable("a", BigInteger.ONE)));
        Map<String, NotSerializable> map = new HashMap<>(Map.of("k", new NotSerializable("b", BigInteger.TWO)));

        List<NotSerializable> listCopy = cloner.clone(list);
        Map<String, NotSerializable> mapCopy = cloner.clone(map);

        assertThat(listCopy).isEqualTo(list).isNotSameAs(list);
        assertThat(listCopy.get(0)).isNotSameAs(list.get(0));
        assertThat(mapCopy).isEqualTo(map).isNotSameAs(map);
    }

    @Test
    void keepsStringsAndNulls() {
        assertThat(cloner.<String>clone("text")).isEqualTo("text");
        assertThat(cloner.<Object>clone(null)).isNull();
    }

    @Test
    void isTheConfiguredJsonSerializer() {
        //hypersistence-utils.properties on the classpath
        assertThat(Configuration.INSTANCE.getProperties().getProperty("hypersistence.utils.json.serializer"))
                .isEqualTo(JsonColumnCloner.class.getName());
    }
}
