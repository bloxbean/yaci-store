package com.bloxbean.cardano.yaci.store.common.config;

import org.springframework.core.env.Environment;
import java.util.LinkedHashMap;
import java.util.Map;

/** Store defaults shared by schema-only initialization and admin snapshot scope resolution. */
public final class StoreModuleConfig {
    private StoreModuleConfig() {}
    public record Module(String property, boolean enabledByDefault, String marker) {}
    public static final Map<String, Module> MODULES;
    static {
        Map<String, Module> modules = new LinkedHashMap<>();
        modules.put("core", new Module(null, true, null));
        add(modules, "blocks", true, "blocks.BlocksStoreConfiguration");
        add(modules, "utxo", true, "utxo.UtxoStoreConfiguration");
        add(modules, "transaction", true, "transaction.TransactionStoreConfiguration");
        add(modules, "script", true, "script.ScriptStoreConfiguration");
        add(modules, "metadata", true, "metadata.MetadataStoreConfiguration");
        add(modules, "assets", true, "assets.AssetsStoreConfiguration");
        add(modules, "epoch", true, "epoch.EpochStoreConfiguration");
        add(modules, "staking", true, "staking.StakingStoreConfiguration");
        add(modules, "mir", true, "mir.MIRStoreConfiguration");
        add(modules, "governance", true, "governance.GovernanceStoreConfiguration");
        add(modules, "account", false, "account.AccountStoreConfiguration");
        add(modules, "adapot", false, "adapot.AdaPotConfiguration");
        add(modules, "epoch-aggr", false, "epochaggr.EpochAggrConfiguration");
        add(modules, "governance-aggr", false, "governanceaggr.GovernanceAggrConfiguration");
        add(modules, "epoch-nonce", false, "epochnonce.EpochNonceConfiguration");
        modules.put("assets-ext", new Module("store.assets.ext.enabled", false,
                "com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtConfiguration"));
        modules.put("analytics-store", new Module("yaci.store.analytics.enabled", false,
                "com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreConfig"));
        MODULES = java.util.Collections.unmodifiableMap(modules);
    }
    private static void add(Map<String, Module> modules, String id, boolean enabled, String marker) {
        modules.put(id, new Module("store." + id + ".enabled", enabled,
                "com.bloxbean.cardano.yaci.store." + marker));
    }
    public static boolean defaultEnabled(String module) {
        return MODULES.get(module).enabledByDefault();
    }
    public static boolean defaultForProperty(String property, boolean fallback) {
        return MODULES.values().stream().filter(m -> property.equals(m.property()))
                .findFirst().map(Module::enabledByDefault).orElse(fallback);
    }
    public static Map<String, Boolean> configured(Environment environment) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        MODULES.forEach((id, module) -> result.put(id, module.property() == null ||
                environment.getProperty(module.property(), Boolean.class, module.enabledByDefault())));
        return result;
    }
    public static Map<String, Boolean> forApplication(Environment environment, ClassLoader loader) {
        Map<String, Boolean> result = configured(environment);
        MODULES.forEach((id, module) -> {
            if (module.marker() != null && !org.springframework.util.ClassUtils.isPresent(module.marker(), loader)) {
                if (module.property() != null && environment.getProperty(module.property(), Boolean.class, false)) {
                    throw new IllegalStateException("Store '" + id + "' is enabled but its module is not installed");
                }
                result.put(id, false);
            }
        });
        return result;
    }
}
