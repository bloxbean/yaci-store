package com.bloxbean.cardano.yaci.store.plugin.polyglot.common;

import com.bloxbean.cardano.yaci.store.common.plugin.ScriptRef;
import org.graalvm.polyglot.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry for script contexts across different languages.
 * This allows scripts in different languages to be registered and accessed globally.
 */
public class GlobalScriptContextRegistry {
    private Map<String, Map<String, Context>> globalScriptContexts = new ConcurrentHashMap<>();
    private Map<String, ScriptRef> scriptRefMap = new ConcurrentHashMap<>();

    public void addScriptRef(String scriptId, ScriptRef scriptRef) {
        scriptRefMap.put(scriptId, scriptRef);
    }

    public ScriptRef getScriptRef(String scriptId) {
        return scriptRefMap.get(scriptId);
    }

    public void addScriptContext(String langId, String scriptId, Context ctx) {
        var langContextMap = globalScriptContexts.get(langId);
        if (langContextMap == null) {
            langContextMap = new ConcurrentHashMap<>();
            globalScriptContexts.put(langId, langContextMap);
        }

        langContextMap.put(scriptId, ctx);
    }

    public Context getScriptContext(String langId, String scriptId) {
        var langContextMap = globalScriptContexts.get(langId);
        if (langContextMap != null) {
            return langContextMap.get(scriptId);
        }
        return null;
    }
}
