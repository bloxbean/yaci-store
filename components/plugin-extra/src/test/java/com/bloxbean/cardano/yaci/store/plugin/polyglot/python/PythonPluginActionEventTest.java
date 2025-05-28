package com.bloxbean.cardano.yaci.store.plugin.polyglot.python;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheConfig;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PythonPluginActionEventTest {

    static PluginCacheConfig pluginCacheConfig;
    static PluginCacheService pluginCacheService;

    @BeforeAll
    static void setup() {
        pluginCacheConfig = new PluginCacheConfig();
        pluginCacheService = new PluginCacheService(pluginCacheConfig.globalCache(),
                pluginCacheConfig.pluginCaches());
    }

    @Test
    void preActionByExpression_inlineScript() {
        PythonPolyglotPluginFactory factory = new PythonPolyglotPluginFactory(null, pluginCacheService);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setType("python");
        pluginDef.setInlineScript(
"""
result = []
items[0].setOwnerAddr('xyz')
print(items[0])
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
        PythonPolyglotPluginFactory factory = new PythonPolyglotPluginFactory(null, pluginCacheService);

        // Create a template file with script content
        String scriptContent =
"""
result = []
items[0].setOwnerAddr('xyz')
print(items[0])           
""";

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("python_script", ".py");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setType("python");
        pluginDef.setScript(
                new PluginDef.Script(tempScriptFile.toFile().getAbsolutePath(), null)
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

        preAction.preAction(List.of(addressUtxo1, addressUtxo2));

        assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("xyz");
        assertThat(addressUtxo2.getOwnerAddr()).isEqualTo("addr_test1qrelw0xltnssmf3fv2wvv4z4zdu4lyndt7n4tf2khv6w3sfnarzvgpra35g3xw5qksknguv5qs0n8hsjqw243gave4fqqlrp9j");
    }

    @Test
    void postActionByExpression_inlineScript() {
        PythonPolyglotPluginFactory factory = new PythonPolyglotPluginFactory(null, pluginCacheService);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setType("python");
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
        PythonPolyglotPluginFactory factory = new PythonPolyglotPluginFactory(null, pluginCacheService);

        // Create a template file with script content
        String scriptContent =
                """
                result = []
                items[0].setOwnerAddr('xyz')
                print(items[0])           
                """;

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("python_script", ".py");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setType("python");
        pluginDef.setScript(
                new PluginDef.Script(tempScriptFile.toFile().getAbsolutePath(), null)
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
        PythonPolyglotPluginFactory factory = new PythonPolyglotPluginFactory(null, pluginCacheService);

        String initScriptContent = """
def __init():
    print("This is a method in init script")
    return "some_value"
                """;
        Path initScriptFile = Files.createTempFile("boot_strap_python_script", ".py");
        Files.writeString(initScriptFile, initScriptContent);

        PluginDef initDef = new PluginDef();
        initDef.setName("py_init");
        initDef.setType("python");
        initDef.setScript(new PluginDef.Script(initScriptFile.toFile().getAbsolutePath(), null));

        factory.createInitPlugin(initDef).init();

        // Create a template file with script content
        String scriptContent =
"""
event.setOwnerAddr('xyz')
print("Inside event handler");  
print("Init value: ", __init)
""";

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("python_script", ".py");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test");
        pluginDef.setType("python");
        pluginDef.setScript(
                new PluginDef.Script(tempScriptFile.toFile().getAbsolutePath(), null)
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
    void eventHandler_script_withInlineInitScript() throws Exception {
        PythonPolyglotPluginFactory factory = new PythonPolyglotPluginFactory(null, pluginCacheService);

        PluginDef initDef = new PluginDef();
        initDef.setName("test");
        initDef.setType("python");
        initDef.setInlineScript("""
greet = "Hello from inline init script"
def __init():
    print("Python           plugin initialized")
    return "init_value"                   
                """);

        factory.createInitPlugin(initDef).init();

        // Create a template file with script content
        String scriptContent =
                """              
                print("Inside event handler");  
                val = __init
                print("Init value: ", val)
                event.setOwnerAddr(val)
                """;

        // Create a temporary file with script content
        Path tempScriptFile = Files.createTempFile("python_script", ".py");
        Files.writeString(tempScriptFile, scriptContent);

        List<Thread> threads = new ArrayList<>();
        for (int i=0; i<10; i++) {
            var t = Thread.startVirtualThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Running event handler in thread: " + Thread.currentThread());
                    PluginDef pluginDef = new PluginDef();
                    pluginDef.setName("test");
                    pluginDef.setType("python");
                    pluginDef.setScript(
                            new PluginDef.Script(tempScriptFile.toFile().getAbsolutePath(), null)
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
                    System.out.println(addressUtxo1.getOwnerAddr());

                    assertThat(addressUtxo1.getOwnerAddr()).isEqualTo("init_value");
                }
            });

            threads.add(t);
        }

        for (Thread t : threads) {
            t.join();
        }


    }

}
