package com.bloxbean.cardano.yaci.store.plugin.polyglot.js;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.config.ScriptDef;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.BasePluginTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsPluginActionEventTest extends BasePluginTest {

    @Test
    void preActionByExpression_inlineScript() {
        JsPolyglotPluginFactory factory = getPluginFactory();

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setLang("js");
        pluginDef.setInlineScript(
"""
    result = []
items[0].setOwnerAddr('xyz')
print(items[0])

result.push(items[0])
result.push(items[1])

return result
"""
        );
        var action = factory.createPreActionPlugin(pluginDef);

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

        action.preAction(List.of(addressUtxo1, addressUtxo2));

        assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("xyz");
        assertThat(addressUtxo2.getOwnerAddr()).isEqualTo("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
    }

    @Test
    void preActionByExpression_script() throws Exception {
        JsPolyglotPluginFactory factory = getPluginFactory();

        // Create a template file with script content
        String scriptContent =
"""
let result = []
items[0].setOwnerAddr('xyz')
print(items[0])
result.push(items[0])
result.push(items[1])

return result
           
""";

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("js_script", ".js");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setLang("js");
        pluginDef.setScript(
                new ScriptDef(null, tempScriptFile.toFile().getAbsolutePath(), null)
        );
        var preAction = factory.createPreActionPlugin(pluginDef);

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

        var result = preAction.preAction(List.of(addressUtxo1, addressUtxo2));
        var iterator = result.iterator();
        AddressUtxo fistObject = (AddressUtxo) iterator.next();
        AddressUtxo secondObject = (AddressUtxo) iterator.next();


        assertThat(fistObject.getOwnerAddr()).isEqualTo("xyz");
        assertThat(secondObject.getOwnerAddr()).isEqualTo("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
    }

    @Test
    void postActionByExpression_inlineScript() {
        JsPolyglotPluginFactory factory = getPluginFactory();

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setLang("js");
        pluginDef.setInlineScript(
                """
                result = []
                items[0].setOwnerAddr('xyz')
                print(items[0])
                """
        );
        var action = factory.createPostActionPlugin(pluginDef);

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

        action.postAction(List.of(addressUtxo1, addressUtxo2));

        assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("xyz");
        assertThat(addressUtxo2.getOwnerAddr()).isEqualTo("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
    }

    @Test
    void postActionByExpression_script() throws Exception {
        JsPolyglotPluginFactory factory = getPluginFactory();

        // Create a template file with script content
        String scriptContent =
                """
                result = []
                items[0].setOwnerAddr('xyz')
                print(items[0])           
                """;

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("js_script", ".js");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setLang("js");
        pluginDef.setScript(
                new ScriptDef(null, tempScriptFile.toFile().getAbsolutePath(), null)
        );
        var postAction = factory.createPostActionPlugin(pluginDef);

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

        postAction.postAction(List.of(addressUtxo1, addressUtxo2));

        assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("xyz");
        assertThat(addressUtxo2.getOwnerAddr()).isEqualTo("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
    }

    @Test
    void eventHandler_script() throws Exception {
        JsPolyglotPluginFactory factory = getPluginFactory();

        // Create a template file with script content
        String scriptContent =
"""
event.setOwnerAddr('xyz')
print("Inside event handler");        
""";

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("js_script", ".js");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setLang("js");
        pluginDef.setScript(
                new ScriptDef(null, tempScriptFile.toFile().getAbsolutePath(), null)
        );
        var eventHandler = factory.createEventHandlerPlugin(pluginDef);

        //Workaround: Instead of using event, we are using single AddressUtxo object
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

        eventHandler.handleEvent(addressUtxo1);

        assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("xyz");
    }

    @Test
    void eventHandler_script_withInitScript() throws Exception {
        JsPolyglotPluginFactory factory = getPluginFactory();

        String initScriptContent = """
        function __init() {
            print("This is a method in init script");
            return "some_init_value_";
        }
                """;
        Path initScriptFile = Files.createTempFile("boot_strap_js_script", ".js");
        Files.writeString(initScriptFile, initScriptContent);

        PluginDef initDef = new PluginDef();
        initDef.setName("js_init");
        initDef.setLang("js");
        initDef.setScript(new ScriptDef(null, initScriptFile.toFile().getAbsolutePath(), null));

        factory.createInitPlugin(initDef).initPlugin();

        // Create a template file with script content
        String scriptContent =
                """
                event.setOwnerAddr(__init)
                print("Inside event handler");  
                print("Init value: ", __init)
                """;

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("js_script", ".js");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setLang("js");
        pluginDef.setScript(
                new ScriptDef(null, tempScriptFile.toFile().getAbsolutePath(), null)
        );
        var eventHandler = factory.createEventHandlerPlugin(pluginDef);

        //Workaround: Instead of using event, we are using single AddressUtxo object
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

        eventHandler.handleEvent(addressUtxo1);

        assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("some_init_value_");
    }

    @Test
    void eventHandler_script_withInitInlineScript() throws Exception {
        JsPolyglotPluginFactory factory = getPluginFactory();

        PluginDef initDef = new PluginDef();
        initDef.setName("js_init");
        initDef.setLang("js");
        initDef.setInlineScript("""
        function __init() {
            print("This is a method in init script");
            return "some_init_value_";
        }
                """);

        factory.createInitPlugin(initDef).initPlugin();

        // Create a template file with script content
        String scriptContent =
                """
                event.setOwnerAddr(__init)
                print("Inside event handler");  
                print("Init value: ", __init)
                """;

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("js_script", ".js");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setLang("js");
        pluginDef.setScript(
                new ScriptDef(null, tempScriptFile.toFile().getAbsolutePath(), null)
        );
        var eventHandler = factory.createEventHandlerPlugin(pluginDef);

        //Workaround: Instead of using event, we are using single AddressUtxo object
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

        eventHandler.handleEvent(addressUtxo1);

        assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("some_init_value_");
    }

    @Test
    void eventHandler_script_withInitInlineScript_multithread() throws Exception {
        JsPolyglotPluginFactory factory = getPluginFactory();

        PluginDef initDef = new PluginDef();
        initDef.setName("js_init");
        initDef.setLang("js");
        initDef.setInlineScript("""
        function __init() {
            print("This is a method in init script");
            return "some_init_value_";
        }
                """);

        factory.createInitPlugin(initDef).initPlugin();

        // Create a template file with script content
        String scriptContent =
                """
                event.setOwnerAddr(__init)
                print("Inside event handler");  
                print("Init value: ", __init)
                """;

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("js_script", ".js");
        Files.writeString(tempScriptFile, scriptContent);

        List<Thread> threads = new ArrayList<>();
        for (int i=0; i < 10; i++) {

            var thread = Thread.startVirtualThread(new Runnable() {
                @Override
                public void run() {
                    PluginDef pluginDef = new PluginDef();
                    pluginDef.setName("test");
                    pluginDef.setLang("js");
                    pluginDef.setScript(
                            new ScriptDef(null, tempScriptFile.toFile().getAbsolutePath(), null)
                    );
                    var eventHandler = factory.createEventHandlerPlugin(pluginDef);

                    //Workaround: Instead of using event, we are using single AddressUtxo object
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

                    eventHandler.handleEvent(addressUtxo1);

                    assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("some_init_value_");
                }
            });

            threads.add(thread);
        }

        // Wait for a while to let all threads complete
        for (var thread: threads)
            thread.join();
    }

    private JsPolyglotPluginFactory getPluginFactory() {
        return new JsPolyglotPluginFactory(null, pluginCacheService, variableProviderFactory, contextProvider, globalScriptContextRegistry);
    }
}
