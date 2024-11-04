package com.bloxbean.cardano.yaci.store.filter;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class EnhancedGraalvmPlugin implements Plugin {
    private final String pluginId;
    private final String scriptFilePath;
    private final String language;
    private String scriptContent;

    public EnhancedGraalvmPlugin(String pluginId, String scriptFilePath, String language) {
        this.pluginId = pluginId;
        this.scriptFilePath = scriptFilePath;
        this.language = language;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public void initialize() {
        try {
            // Read script content from file once during initialization
            Path path = Paths.get(scriptFilePath);
            if (!Files.exists(path)) {
                throw new IOException("Script file not found: " + scriptFilePath);
            }
            scriptContent = Files.readString(path);
            System.out.println("Plugin " + pluginId + " initialized with script from: " + scriptFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize plugin " + pluginId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Object execute(Object data) {
        // Create and close a new Context for each execution to avoid reuse issues
        try (Context context = Context.newBuilder().allowAllAccess(true).build()) {
            // Evaluate the script to load it into the context
            context.eval(language, scriptContent);

            // Retrieve the function by name and ensure it is callable
            Value function = context.getBindings(language).getMember("filterData");
            if (function != null && function.canExecute()) {
                return function.execute(data).as(Object.class);
            } else {
                throw new IllegalStateException("The function 'filterData' is not defined or cannot be executed");
            }
        } catch (Exception e) {
            throw new RuntimeException("Execution of plugin " + pluginId + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        System.out.println("Plugin " + pluginId + " shut down.");
    }
}
