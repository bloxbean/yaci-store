package com.bloxbean.cardano.yaci.store.plugin.impl.mvel;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheConfig;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MvelStorageFilterFactoryTest {
    static PluginCacheConfig pluginCacheConfig;
    static PluginCacheService pluginCacheService;

    @BeforeAll
    static void setup() {
        pluginCacheConfig = new PluginCacheConfig();
        pluginCacheService = new PluginCacheService(pluginCacheConfig.globalCache(),
                pluginCacheConfig.pluginCaches());
    }

    @Test
    void filterListByExpression() {
        MvelStorePluginFactory filterFactory = new MvelStorePluginFactory(pluginCacheService, null);

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setLang("expression");
        filterDef.setExpression("ownerAddr == 'addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j'");

        var filter = filterFactory.<AddressUtxo>createFilterPlugin(filterDef);

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
    void filterListByExpression_arrayField() {
        MvelStorePluginFactory filterFactory = new MvelStorePluginFactory(pluginCacheService, null);

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setLang("mvel");
        filterDef.setExpression("(policyId in amounts).contains('policyId1')");
        var filter = filterFactory.<AddressUtxo>createFilterPlugin(filterDef);

        AddressUtxo addressUtxo1 = new AddressUtxo();
        addressUtxo1.setOwnerAddr("addrabc");
        addressUtxo1.setTxHash("txHash1");
        addressUtxo1.setAmounts(List.of(
                Amt.builder()
                        .policyId("policyId1")
                        .assetName("assetName1")
                        .build(),
                Amt.builder()
                        .policyId("policyId2")
                        .assetName("assetName2")
                        .build()
        ));

        AddressUtxo addressUtxo2 = new AddressUtxo();
        addressUtxo2.setOwnerAddr("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
        addressUtxo2.setTxHash("txHash2");
        addressUtxo2.setAmounts(List.of(
                Amt.builder()
                        .policyId("apolicyId1")
                        .assetName("aassetName1")
                        .build(),
                Amt.builder()
                        .policyId("apolicyId2")
                        .assetName("aassetName2")
                        .build()
        ));

        var result = filter.filter(List.of(addressUtxo1, addressUtxo2));

        assertThat(result).hasSize(1);
    }

    @Test
    void filterListByExpression_withScript() {
        MvelStorePluginFactory filterFactory = new MvelStorePluginFactory(pluginCacheService, null);

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setLang("mvel");
        filterDef.setInlineScript("""
                            result = [];
                            for (item : items) {
                                for (amt : item.amounts) {
                                    if ( amt.policyId == 'policyId1' && amt.assetName == 'assetName1') {
                                        result.add(item);
                                    }
                                }                             
                            }
                            result
                """);
        var filter = filterFactory.createFilterPlugin(filterDef);

        AddressUtxo addressUtxo1 = new AddressUtxo();
        addressUtxo1.setOwnerAddr("addrabc");
        addressUtxo1.setTxHash("txHash1");
        addressUtxo1.setAmounts(List.of(
                Amt.builder()
                        .policyId("policyId1")
                        .assetName("assetName1")
                        .build(),
                Amt.builder()
                        .policyId("policyId2")
                        .assetName("assetName2")
                        .build()
        ));

        AddressUtxo addressUtxo2 = new AddressUtxo();
        addressUtxo2.setOwnerAddr("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
        addressUtxo2.setTxHash("txHash2");
        addressUtxo2.setAmounts(List.of(
                Amt.builder()
                        .policyId("apolicyId1")
                        .assetName("aassetName1")
                        .build(),
                Amt.builder()
                        .policyId("apolicyId2")
                        .assetName("aassetName2")
                        .build()
        ));

        var result = filter.filter(List.of(addressUtxo1, addressUtxo2));

        assertThat(result).hasSize(1);
    }

    @Test
    void filterListByExpression_withScript_noResult() {
        MvelStorePluginFactory filterFactory = new MvelStorePluginFactory(pluginCacheService, null);

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setLang("mvel");
        filterDef.setInlineScript("""
                            result = [];
                            for (item : items) {
                                for (amt : item.amounts) {
                                    if ( amt.policyId == 'policyId1' && amt.assetName == 'assetNamexxxx') {
                                        result.add(item);
                                    }
                                }                             
                            }
                            result
                """);
        var filter = filterFactory.createFilterPlugin(filterDef);

        AddressUtxo addressUtxo1 = new AddressUtxo();
        addressUtxo1.setOwnerAddr("addrabc");
        addressUtxo1.setTxHash("txHash1");
        addressUtxo1.setAmounts(List.of(
                Amt.builder()
                        .policyId("policyId1")
                        .assetName("assetName1")
                        .build(),
                Amt.builder()
                        .policyId("policyId2")
                        .assetName("assetName2")
                        .build()
        ));

        AddressUtxo addressUtxo2 = new AddressUtxo();
        addressUtxo2.setOwnerAddr("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
        addressUtxo2.setTxHash("txHash2");
        addressUtxo2.setAmounts(List.of(
                Amt.builder()
                        .policyId("apolicyId1")
                        .assetName("aassetName1")
                        .build(),
                Amt.builder()
                        .policyId("apolicyId2")
                        .assetName("aassetName2")
                        .build()
        ));

        var result = filter.filter(List.of(addressUtxo1, addressUtxo2));

        assertThat(result).isEmpty();
    }

    @Test
    void preActionListByScript_updateAttribute() {
        MvelStorePluginFactory filterFactory = new MvelStorePluginFactory(pluginCacheService, null);

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setLang("mvel");
        filterDef.setInlineScript("""                        
                            for (item : items) {
                                if (item.ownerAddr == 'addrabc')
                                    item.ownerAddr = 'addrxyz'                             
                            }
                                                       
                """);
        var preActionPlugin = filterFactory.createPreActionPlugin(filterDef);

        AddressUtxo addressUtxo1 = new AddressUtxo();
        addressUtxo1.setOwnerAddr("addrabc");
        addressUtxo1.setTxHash("txHash1");
        addressUtxo1.setAmounts(List.of(
                Amt.builder()
                        .policyId("policyId1")
                        .assetName("assetName1")
                        .build(),
                Amt.builder()
                        .policyId("policyId2")
                        .assetName("assetName2")
                        .build()
        ));

        AddressUtxo addressUtxo2 = new AddressUtxo();
        addressUtxo2.setOwnerAddr("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
        addressUtxo2.setTxHash("txHash2");
        addressUtxo2.setAmounts(List.of(
                Amt.builder()
                        .policyId("apolicyId1")
                        .assetName("aassetName1")
                        .build(),
                Amt.builder()
                        .policyId("apolicyId2")
                        .assetName("aassetName2")
                        .build()
        ));

        preActionPlugin.preAction(List.of(addressUtxo1, addressUtxo2));

        assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("addrxyz");
    }

    @Test
    void postActionByScript_updateAttribute() {
        MvelStorePluginFactory filterFactory = new MvelStorePluginFactory(pluginCacheService, null);

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setLang("mvel");
        filterDef.setInlineScript("""                        
                            for (item : items) {
                                if (item.ownerAddr == "addrabc") {
                                    item.ownerAddr = "addrxyz";
                                    item.txHash = null;
                                }                         
                            }
                                                       
                """);
        var postActionPlugin = filterFactory.createPostActionPlugin(filterDef);

        AddressUtxo addressUtxo1 = new AddressUtxo();
        addressUtxo1.setOwnerAddr("addrabc");
        addressUtxo1.setTxHash("txHash1");
        addressUtxo1.setAmounts(List.of(
                Amt.builder()
                        .policyId("policyId1")
                        .assetName("assetName1")
                        .build(),
                Amt.builder()
                        .policyId("policyId2")
                        .assetName("assetName2")
                        .build()
        ));

        AddressUtxo addressUtxo2 = new AddressUtxo();
        addressUtxo2.setOwnerAddr("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
        addressUtxo2.setTxHash("txHash2");
        addressUtxo2.setAmounts(List.of(
                Amt.builder()
                        .policyId("apolicyId1")
                        .assetName("aassetName1")
                        .build(),
                Amt.builder()
                        .policyId("apolicyId2")
                        .assetName("aassetName2")
                        .build()
        ));

        postActionPlugin.postAction(List.of(addressUtxo1, addressUtxo2));

        assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("addrxyz");
        assertThat(addressUtxo1.getTxHash()).isNull();
    }
}

