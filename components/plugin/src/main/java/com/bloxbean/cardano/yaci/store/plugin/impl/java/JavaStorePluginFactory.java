package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.api.EventHandlerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.InitPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginFactory;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.PostActionPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PreActionPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.SchedulerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.config.ScriptRef;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

/**
 * {@link PluginFactory} for {@code lang=java}.
 *
 * <p>Instead of a script or expression, the {@link PluginDef#getClassName() class-name} field
 * provides the fully-qualified name of a user-written Java class. The factory loads and
 * instantiates that class and registers it as the requested plugin type.</p>
 *
 * <p>The class must implement the plugin interface matching its config section — e.g. a class
 * referenced under {@code store.plugins.filters} must implement {@link FilterPlugin}, one under
 * {@code store.plugins.schedulers} must implement {@link SchedulerPlugin}. The easiest way is to
 * extend one of the base classes in this package ({@link JavaFilterPlugin},
 * {@link JavaPreActionPlugin}, {@link JavaPostActionPlugin}, {@link JavaEventHandlerPlugin},
 * {@link JavaInitPlugin}, {@link JavaSchedulerPlugin}), which implement the {@link
 * com.bloxbean.cardano.yaci.store.plugin.api.IPlugin} bookkeeping and expose a constructor
 * {@code (PluginDef, PluginType, PluginContext)}.</p>
 *
 * <p>Constructor selection (first match wins):
 * <ol>
 *   <li>{@code (PluginDef, PluginType, PluginContext)} — preferred, used by the base classes</li>
 *   <li>{@code (PluginContext)}</li>
 *   <li>no-arg</li>
 * </ol>
 * For (2) and (3) the class is responsible for implementing the {@link
 * com.bloxbean.cardano.yaci.store.plugin.api.IPlugin} bookkeeping itself.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JavaStorePluginFactory implements PluginFactory {

    private final PluginStateService pluginStateService;
    private final VariableProviderFactory variableProviderFactory;
    private final PluginContextUtil pluginContextUtil;
    private final ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        log.info("Java Plugin Factory created >>");
    }

    @Override
    public String getLang() {
        return "java";
    }

    @Override
    public void initGlobalScripts(List<ScriptRef> scriptRef) {
        // no-op — java plugins are classes, not scripts
    }

    @Override
    public <T> InitPlugin<T> createInitPlugin(PluginDef def) {
        return instantiate(def, PluginType.INIT, InitPlugin.class);
    }

    @Override
    public <T> FilterPlugin<T> createFilterPlugin(PluginDef def) {
        return instantiate(def, PluginType.FILTER, FilterPlugin.class);
    }

    @Override
    public <T> PreActionPlugin<T> createPreActionPlugin(PluginDef def) {
        return instantiate(def, PluginType.PRE_ACTION, PreActionPlugin.class);
    }

    @Override
    public <T> PostActionPlugin<T> createPostActionPlugin(PluginDef def) {
        return instantiate(def, PluginType.POST_ACTION, PostActionPlugin.class);
    }

    @Override
    public <T> EventHandlerPlugin<T> createEventHandlerPlugin(PluginDef def) {
        return instantiate(def, PluginType.EVENT_HANDLER, EventHandlerPlugin.class);
    }

    @Override
    public <T> SchedulerPlugin<T> createSchedulerPlugin(PluginDef def) {
        return instantiate(def, PluginType.SCHEDULER, SchedulerPlugin.class);
    }

    @SuppressWarnings("unchecked")
    private <P> P instantiate(PluginDef def, PluginType type, Class<P> pluginInterface) {
        String className = def.getClassName();
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException(
                    "'class-name' is required for lang=java plugin (" + type + "). Def: " + def);
        }

        Class<?> clazz;
        try {
            clazz = Class.forName(className.trim());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Java plugin class not found: " + className + ". Ensure it is on the classpath " +
                            "(e.g. bundled in a JAR under plugins/ext-jars, loaded via -Dloader.path).", e);
        }

        Object instance = newInstance(clazz, def, type);

        if (!pluginInterface.isInstance(instance)) {
            throw new ClassCastException("Java plugin class " + clazz.getName()
                    + " does not implement " + pluginInterface.getSimpleName()
                    + " (required for " + type + " plugins in the '"
                    + configSection(type) + "' config section). Def: " + def);
        }

        log.info("Created java {} plugin '{}' from class {}", type, def.getName(), clazz.getName());
        return (P) instance;
    }

    private Object newInstance(Class<?> clazz, PluginDef def, PluginType type) {
        // 1) Preferred: (PluginDef, PluginType, PluginContext) — used by the Java* base classes.
        Constructor<?> ctor = findConstructor(clazz, PluginDef.class, PluginType.class, PluginContext.class);
        if (ctor != null) {
            return invoke(ctor, def, type, buildContext(def));
        }

        // 2) Fallback: (PluginContext) only.
        ctor = findConstructor(clazz, PluginContext.class);
        if (ctor != null) {
            return invoke(ctor, buildContext(def));
        }

        // 3) Fallback: no-arg. The class must implement IPlugin bookkeeping itself.
        ctor = findConstructor(clazz);
        if (ctor != null) {
            return invoke(ctor);
        }

        throw new IllegalStateException("Java plugin " + clazz.getName() + " has no usable constructor. "
                + "Declare (PluginDef, PluginType, PluginContext) — the easiest way is to extend "
                + "one of the Java*Plugin base classes in " + AbstractJavaPlugin.class.getPackageName() + ".");
    }

    private Constructor<?> findConstructor(Class<?> clazz, Class<?>... paramTypes) {
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (Arrays.equals(c.getParameterTypes(), paramTypes)) {
                return c;
            }
        }
        return null;
    }

    private Object invoke(Constructor<?> ctor, Object... args) {
        try {
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("Failed to instantiate java plugin via " + ctor + ": "
                    + cause.getMessage(), cause);
        }
    }

    private PluginContext buildContext(PluginDef def) {
        return new DefaultPluginContext(
                def.getName(), pluginStateService, variableProviderFactory, pluginContextUtil, applicationContext);
    }

    private static String configSection(PluginType type) {
        return switch (type) {
            case INIT -> "init";
            case FILTER -> "filters";
            case PRE_ACTION -> "pre-actions";
            case POST_ACTION -> "post-actions";
            case EVENT_HANDLER -> "event-handlers";
            case SCHEDULER -> "schedulers";
        };
    }
}
