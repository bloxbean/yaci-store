package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.cache.State;
import com.bloxbean.cardano.yaci.store.plugin.file.PluginFileClient;
import com.bloxbean.cardano.yaci.store.plugin.http.PluginHttpClient;
import com.bloxbean.cardano.yaci.store.plugin.util.Locker;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Typed facade passed to {@code lang=java} plugins. It exposes the same services that the script
 * languages (MVEL/JS/Python) receive as loose variables — {@code jdbc}, {@code named_jdbc},
 * {@code rest}, {@code env}, {@code http}, {@code files}, {@code locker}, {@code state},
 * {@code global_state} and the {@code *_reader} beans — but in a typed, IDE-friendly way.
 *
 * <p>Java plugins obtain a {@link PluginContext} through their constructor (see
 * {@link AbstractJavaPlugin}); the {@link JavaStorePluginFactory} builds one per plugin
 * definition so that {@link #state()} resolves to the correct per-plugin state.</p>
 */
public interface PluginContext {

    /** The plugin name (from {@code PluginDef.getName()}), used as the per-plugin state key. */
    String getPluginName();

    /** Per-plugin state (equivalent to the {@code state} variable in scripts). */
    State<String, Object> state();

    /** Global state shared across all plugins (equivalent to {@code global_state}). */
    State<String, Object> globalState();

    /** Full, read-only variable map (the same one script plugins receive). */
    Map<String, Object> variables();

    PluginHttpClient http();

    PluginFileClient files();

    Locker locker();

    JdbcTemplate jdbc();

    NamedParameterJdbcTemplate namedJdbc();

    RestTemplate restTemplate();

    Environment environment();

    /** Look up any Spring bean by type (e.g. a storage reader). */
    <T> T bean(Class<T> type);

    /** Look up any Spring bean by name (e.g. {@code "asset_reader"}). */
    Object bean(String name);

    /** The underlying Spring context, for advanced cases. */
    ApplicationContext applicationContext();
}
