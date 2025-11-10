package com.bloxbean.cardano.yaci.store.plugin.polyglot.python;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.BasePluginTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that Python filter plugins execute only ONCE per invocation.
 *
 * This test will FAIL before the fix (executionCount = 2)
 * This test will PASS after the fix (executionCount = 1)
 */
class PythonFilterPluginExecutionCountTest extends BasePluginTest {

    @Test
    void filterPlugin_shouldExecuteOnlyOnce_notTwice() {
        // Create factory
        PythonPolyglotPluginFactory filterFactory = new PythonPolyglotPluginFactory(
            null,
            pluginCacheService,
            variableProviderFactory,
            contextProvider,
            globalScriptContextRegistry
        );

        // Create filter plugin that tracks execution count using state
        PluginDef filterDef = new PluginDef();
        filterDef.setName("py-execution-count-test");
        filterDef.setLang("python");
        filterDef.setInlineScript("""
# Track how many times this filter executes
execution_count = state.get('executionCount') or 0
execution_count = execution_count + 1
state.put('executionCount', execution_count)

# Log for debugging
print(f'Filter executed: {execution_count} times')

# Return all items unchanged
return items
        """);

        FilterPlugin<AddressUtxo> filter = filterFactory.createFilterPlugin(filterDef);

        // Create test data
        AddressUtxo utxo1 = new AddressUtxo();
        utxo1.setOwnerAddr("addr1");
        utxo1.setTxHash("txHash1");
        utxo1.setAmounts(List.of(
            Amt.builder()
                .policyId("policy1")
                .assetName("asset1")
                .build()
        ));

        AddressUtxo utxo2 = new AddressUtxo();
        utxo2.setOwnerAddr("addr2");
        utxo2.setTxHash("txHash2");
        utxo2.setAmounts(List.of(
            Amt.builder()
                .policyId("policy2")
                .assetName("asset2")
                .build()
        ));

        List<AddressUtxo> testItems = List.of(utxo1, utxo2);

        // Execute filter ONCE
        var result = filter.filter(testItems);

        // Verify result is correct
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(utxo1, utxo2);

        // Verify execution count
        // BUG: This will be 2 before the fix (filter executes twice)
        // FIXED: This should be 1 after the fix (filter executes once)
        var executionCount = pluginCacheService.forPlugin("py-execution-count-test")
            .get("executionCount");

        assertThat(executionCount)
            .as("Filter should execute ONCE per filter() call, not twice")
            .isEqualTo(1);
    }

    @Test
    void filterPlugin_multipleInvocations_shouldIncrementCorrectly() {
        // Create factory
        PythonPolyglotPluginFactory filterFactory = new PythonPolyglotPluginFactory(
            null,
            pluginCacheService,
            variableProviderFactory,
            contextProvider,
            globalScriptContextRegistry
        );

        // Create filter plugin
        PluginDef filterDef = new PluginDef();
        filterDef.setName("py-multi-invocation-test");
        filterDef.setLang("python");
        filterDef.setInlineScript("""
count = state.get('count') or 0
count = count + 1
state.put('count', count)
return items
        """);

        FilterPlugin<AddressUtxo> filter = filterFactory.createFilterPlugin(filterDef);

        // Create test data
        AddressUtxo utxo = new AddressUtxo();
        utxo.setOwnerAddr("addr1");
        utxo.setTxHash("txHash1");
        List<AddressUtxo> testItems = List.of(utxo);

        // Call filter 3 times
        filter.filter(testItems);
        filter.filter(testItems);
        filter.filter(testItems);

        // Verify count
        // BUG: This will be 6 before fix (2 executions x 3 calls)
        // FIXED: This should be 3 after fix (1 execution x 3 calls)
        var count = pluginCacheService.forPlugin("py-multi-invocation-test")
            .get("count");

        assertThat(count)
            .as("3 filter() calls should result in 3 executions, not 6")
            .isEqualTo(3);
    }
}
