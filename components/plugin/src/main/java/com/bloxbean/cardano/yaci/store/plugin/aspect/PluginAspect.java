package com.bloxbean.cardano.yaci.store.plugin.aspect;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.core.PluginRegistry;
import com.bloxbean.cardano.yaci.store.plugin.api.PostActionPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PreActionPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.metrics.PluginMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        value = "store.plugins.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PluginAspect {

    private final StoreProperties storeProperties;
    private final PluginRegistry pluginRegistry;
    private final PluginMetricsCollector metricsCollector;

    @Around("@annotation(plugin)")
    public Object aroundSaveAll(ProceedingJoinPoint pjp, Plugin plugin) throws Throwable {
        if (!storeProperties.isPluginsEnabled())
            return null;

        Object[] args = pjp.getArgs();
        if (args.length == 0) {
            return pjp.proceed();
        }

        String key  = plugin.key();

        List<PreActionPlugin<?>> preActions = pluginRegistry.getPreActionPlugins(key);
        List<FilterPlugin<?>> filters = pluginRegistry.getFilterPlugins(key);
        List<PostActionPlugin<?>> postActions = pluginRegistry.getPostActionPlugins(key);

        if (preActions.isEmpty() && filters.isEmpty() && postActions.isEmpty()) {
            return pjp.proceed();
        }

        boolean singleObject = !(args[0] instanceof Collection<?>);
        Collection<Object> items;

        if (singleObject) {
            items = new ArrayList<>();
            items.add(args[0]);
        } else {
            Collection<Object> c = (Collection<Object>) args[0];
            items = c;
        }

        for (FilterPlugin<?> f : filters) {
            FilterPlugin<Object> sf = (FilterPlugin<Object>) f;
            long startTime = System.currentTimeMillis();
            int initialSize = items.size();
            boolean success = false;

            try {
                items = sf.filter(items);
                success = true;

                // Record items processed
                metricsCollector.recordItemsProcessed(f.getName(), initialSize);

            } catch (Exception e) {
                if (shouldExitOnError(f.getPluginDef())) {
                    throw e;
                }
            } finally {
                // Record execution metrics
                long endTime = System.currentTimeMillis();
                metricsCollector.recordExecution(
                    f.getName(),
                    PluginType.FILTER,
                    f.getPluginDef().getLang(),
                    startTime,
                    endTime,
                    success,
                    null
                );
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Applied {} filters on {} items for {}.{}",
                    filters.size(), items.size(),
                    pjp.getSignature().getDeclaringType().getSimpleName(),
                    pjp.getSignature().getName());
        }

        if (items.isEmpty()) {
            return null;
        }

        // Apply pre-filters
        if (!preActions.isEmpty()) {
            for (PreActionPlugin preStoreFilter: preActions) {
                long startTime = System.currentTimeMillis();
                boolean success = false;

                try {
                    preStoreFilter.preAction(items);
                    success = true;

                    // Record items processed
                    metricsCollector.recordItemsProcessed(preStoreFilter.getName(), items.size());

                } catch (Exception e) {
                    //TODO : Should we throw exception or just log it?
                    log.error("Error executing pre-filter {}: {}", preStoreFilter.getName(), e.getMessage());
                    if (shouldExitOnError(preStoreFilter.getPluginDef())) {
                        throw e;
                    }
                } finally {
                    // Record execution metrics
                    long endTime = System.currentTimeMillis();
                    metricsCollector.recordExecution(
                        preStoreFilter.getName(),
                        PluginType.PRE_ACTION,
                        preStoreFilter.getPluginDef().getLang(),
                        startTime,
                        endTime,
                        success,
                        null
                    );
                }
            }
        }


        if (singleObject) {
            args[0] = items.iterator().next();
        } else {
            args[0] = items;
        }

        // Proceed with the original method call
        var returnObj = pjp.proceed(args);

        //Invoke post-filters
        if (!postActions.isEmpty()) {
            for (PostActionPlugin postStoreFilter: postActions) {
                long startTime = System.currentTimeMillis();
                boolean success = false;

                try {
                    postStoreFilter.postAction(items);
                    success = true;

                    // Record items processed
                    metricsCollector.recordItemsProcessed(postStoreFilter.getName(), items.size());

                } catch (Exception e) {
                    //TODO : Should we throw exception or just log it?
                    log.error("Error executing post-filter {}: {}", postStoreFilter.getName(), e.getMessage());
                    if (shouldExitOnError(postStoreFilter.getPluginDef())) {
                        throw e;
                    }
                } finally {
                    // Record execution metrics
                    long endTime = System.currentTimeMillis();
                    metricsCollector.recordExecution(
                        postStoreFilter.getName(),
                        PluginType.POST_ACTION,
                        postStoreFilter.getPluginDef().getLang(),
                        startTime,
                        endTime,
                        success,
                        null
                    );
                }
            }
        }

        return returnObj;
    }

    private boolean shouldExitOnError(PluginDef pluginDef) {
        return pluginDef.getExitOnError() != null
                ? pluginDef.getExitOnError()
                : storeProperties.isPluginExitOnError();
    }
}
