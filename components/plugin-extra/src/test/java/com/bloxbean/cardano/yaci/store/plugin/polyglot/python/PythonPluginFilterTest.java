package com.bloxbean.cardano.yaci.store.plugin.polyglot.python;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.common.plugin.ScriptDef;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.BasePluginTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PythonPluginFilterTest extends BasePluginTest {

    @Test
    void filterListByExpression_inlineScript() {
        PythonPolyglotPluginFactory filterFactory = getPluginFactory();

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setLang("python");
        filterDef.setInlineScript("""
result = []
for item in items:
    if item.getOwnerAddr() == 'addrabc':            
        result.append(item)
print(result)
                     
return result
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

    private PythonPolyglotPluginFactory getPluginFactory() {
        PythonPolyglotPluginFactory filterFactory = new PythonPolyglotPluginFactory(null, pluginCacheService, variableProviderFactory, contextProvider, globalScriptContextRegistry);
        return filterFactory;
    }

    @Test
    void filterListByExpression_withScript_noFunction() throws Exception {
        PythonPolyglotPluginFactory filterFactory = getPluginFactory();

        // Create a template file with script content
        String scriptContent = """
result = []
for item in items:
    if item.getOwnerAddr() == 'addrabcd':            
        result.append(item)
print(result)

return result            
        """;

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("python_script", ".py");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setLang("python");
        filterDef.setScript(
                new ScriptDef(null, tempScriptFile.toFile().getAbsolutePath(), null)
        );
        var filter = filterFactory.createFilterPlugin(filterDef);

        AddressUtxo addressUtxo1 = new AddressUtxo();
        addressUtxo1.setOwnerAddr("addrabcd");
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
    void filterListByExpression_withScript_withFunction() throws Exception {
        PythonPolyglotPluginFactory filterFactory = getPluginFactory();
        String scriptContent = """
def myfilter(items):
    result = []
    for item in items:
        if item.getOwnerAddr() == 'addrabcd':            
            result.append(item)
    print(result)

    return result            
        """;

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("python_script", ".py");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef filterDef = new PluginDef();
        filterDef.setName("test");
        filterDef.setLang("python");
        filterDef.setScript(
                new ScriptDef(null, tempScriptFile.toFile().getAbsolutePath(), "myfilter")
        );
        var filter = filterFactory.createFilterPlugin(filterDef);

        AddressUtxo addressUtxo1 = new AddressUtxo();
        addressUtxo1.setOwnerAddr("addrabcd");
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
}
