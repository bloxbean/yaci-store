package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.SchedulerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateConfig;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JavaStorePluginFactory}. The test plugin classes at the bottom of this
 * file are loaded by FQCN via reflection, exactly as a real {@code lang=java} plugin would be.
 */
public class JavaStorePluginFactoryTest {

    static PluginStateService pluginStateService;
    static JavaStorePluginFactory factory;

    @BeforeAll
    static void setup() {
        PluginStateConfig config = new PluginStateConfig();
        pluginStateService = new PluginStateService(config.globalState(), config.pluginStates());
        // pluginContextUtil and applicationContext are null here — fine for plugins that only use state()
        factory = new JavaStorePluginFactory(pluginStateService, null, null, null);
    }

    private static PluginDef def(String name, Class<?> clazz) {
        PluginDef d = new PluginDef();
        d.setName(name);
        d.setLang("java");
        d.setClassName(clazz.getName());
        return d;
    }

    @Test
    void getLangIsJava() {
        assertThat(factory.getLang()).isEqualTo("java");
    }

    @Test
    void createFilterPlugin_viaBaseClass() {
        PluginDef d = def("owner-filter", OwnerFilter.class);
        FilterPlugin<AddressUtxo> filter = factory.createFilterPlugin(d);

        assertThat(filter.getName()).isEqualTo("owner-filter");
        assertThat(filter.getPluginType()).isEqualTo(PluginType.FILTER);
        assertThat(filter.getPluginDef()).isSameAs(d);

        AddressUtxo keep = new AddressUtxo();
        keep.setOwnerAddr("keep");
        AddressUtxo drop = new AddressUtxo();
        drop.setOwnerAddr("drop");

        Collection<AddressUtxo> result = filter.filter(List.of(keep, drop));

        assertThat(result).containsExactly(keep);
        // state() is wired and per-plugin
        assertThat(pluginStateService.forPlugin("owner-filter").get("count")).isEqualTo(1L);
    }

    @Test
    void createSchedulerPlugin_viaBaseClass() {
        PluginDef d = def("counting-scheduler", CountingScheduler.class);
        SchedulerPlugin<Object> scheduler = factory.createSchedulerPlugin(d);

        scheduler.execute();
        scheduler.execute();

        assertThat(pluginStateService.global().get("scheduler-count")).isEqualTo(2L);
    }

    @Test
    void createEventHandlerPlugin_viaBaseClass() {
        PluginDef d = def("capturing-handler", CapturingHandler.class);
        var handler = factory.createEventHandlerPlugin(d);

        handler.handleEvent("hello");

        assertThat(pluginStateService.forPlugin("capturing-handler").get("last-event")).isEqualTo("hello");
    }

    @Test
    void missingClassName_throws() {
        PluginDef d = new PluginDef();
        d.setName("no-class");
        d.setLang("java");

        assertThatThrownBy(() -> factory.createFilterPlugin(d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("class-name");
    }

    @Test
    void classNotFound_throws() {
        PluginDef d = new PluginDef();
        d.setName("missing");
        d.setLang("java");
        d.setClassName("com.example.DoesNotExist");

        assertThatThrownBy(() -> factory.createFilterPlugin(d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void wrongInterface_throws() {
        // OwnerFilter implements FilterPlugin but not SchedulerPlugin
        PluginDef d = def("wrong", OwnerFilter.class);

        assertThatThrownBy(() -> factory.createSchedulerPlugin(d))
                .isInstanceOf(ClassCastException.class)
                .hasMessageContaining("SchedulerPlugin");
    }

    // ---- test plugin classes, referenced by FQCN via reflection ----

    public static class OwnerFilter extends JavaFilterPlugin<AddressUtxo> {
        public OwnerFilter(PluginDef def, PluginType type, PluginContext ctx) {
            super(def, type, ctx);
        }

        @Override
        public Collection<AddressUtxo> filter(Collection<AddressUtxo> items) {
            state().increment("count");
            return items.stream().filter(u -> "keep".equals(u.getOwnerAddr())).toList();
        }
    }

    public static class CountingScheduler extends JavaSchedulerPlugin<Object> {
        public CountingScheduler(PluginDef def, PluginType type, PluginContext ctx) {
            super(def, type, ctx);
        }

        @Override
        public void execute() {
            globalState().increment("scheduler-count");
        }
    }

    public static class CapturingHandler extends JavaEventHandlerPlugin<Object> {
        public CapturingHandler(PluginDef def, PluginType type, PluginContext ctx) {
            super(def, type, ctx);
        }

        @Override
        public void handleEvent(Object event) {
            state().put("last-event", event);
        }
    }
}
