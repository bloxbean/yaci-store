package com.bloxbean.cardano.yaci.store.plugin.filter.spel;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class  SpelStorageFilterFactoryTest {

    @Test
    void filterListByExpression() {
        SpelStorePluginFactory filterFactory = new SpelStorePluginFactory();

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setType("expression");
        filterDef.setExpression("ownerAddr == 'addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j'");

        var filter = filterFactory.createFilterPlugin(filterDef);

        AddressUtxo addressUtxo1 = new AddressUtxo();
        addressUtxo1.setOwnerAddr("addrabc");
        addressUtxo1.setTxHash("txHash1");

        AddressUtxo addressUtxo2 = new AddressUtxo();
        addressUtxo2.setOwnerAddr("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
        addressUtxo2.setTxHash("txHash2");

        var result = filter.filter(List.of(addressUtxo1, addressUtxo2));

        assertThat(result).hasSize(1);
    }

    @Test
    void filterSingleObjectExpression_returnsEmpty() {
        SpelStorePluginFactory filterFactory = new SpelStorePluginFactory();

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setType("expression");
        filterDef.setExpression("slot > 10");

        var filter = filterFactory.createFilterPlugin(filterDef);

        Cursor cursor = Cursor.builder()
                .slot(5L)
                .build();

        var result = filter.filter(List.of(cursor));

        assertThat(result).hasSize(0);
    }

    @Test
    void filterSingleObjectExpression() {
        SpelStorePluginFactory filterFactory = new SpelStorePluginFactory();

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setType("expression");
        filterDef.setExpression("slot >= 5");

        var filter = filterFactory.createFilterPlugin(filterDef);

        Cursor cursor = Cursor.builder()
                .slot(5L)
                .build();

        var result = filter.filter(List.of(cursor));

        assertThat(result).hasSize(1);
    }
}

