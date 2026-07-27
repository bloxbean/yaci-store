package com.bloxbean.example;

import java.util.List;
import java.util.Map;

import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.http.HttpResponseWrapper;
import com.bloxbean.cardano.yaci.store.plugin.http.PluginHttpClient;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.JavaEventHandlerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.PluginContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpExamples extends JavaEventHandlerPlugin<BlockEvent> {

    public HttpExamples(PluginDef pluginDef, PluginType pluginType, PluginContext context) {
        super(pluginDef, pluginType, context);
    }

    public void http() {
        PluginHttpClient http = context().http();

        HttpResponseWrapper response = http.get("https://api.example.com/users");
        if (response.isSuccess()) {
            Map<String, Object> users = response.asJsonMap();
            System.out.println("Found " + users.size() + " users");
        }

        // GET with query parameters
        HttpResponseWrapper resp = http.getWithParams("https://api.example.com/search",
                Map.of("q", "example", "limit", "10"));

        // GET with headers
        HttpResponseWrapper resp2 = http.get("https://api.example.com/data",
                Map.of("User-Agent", "YaciPlugin/1.0", "Accept", "application/json"));

    }

    public void http2() {
        PluginHttpClient http = context().http();

        HttpResponseWrapper response = http.get("https://api.example.com/users");
        if (response.isSuccess()) {
            Map<String, Object> users = response.asJsonMap();
            System.out.println("Found " + users.size() + " users");
        }

        // GET with query parameters
        HttpResponseWrapper resp = http.getWithParams("https://api.example.com/search",
                Map.of("q", "example", "limit", "10"));

        // GET with headers
        HttpResponseWrapper resp2 = http.get("https://api.example.com/data",
                Map.of("User-Agent", "YaciPlugin/1.0", "Accept", "application/json"));

    }

    public void http3() {
        PluginHttpClient http = context().http();

        HttpResponseWrapper response = http.get("https://api.example.com/users");
        if (response.isSuccess()) {
            Map<String, Object> users = response.asJsonMap();
            System.out.println("Found " + users.size() + " users");
        }

        // GET with query parameters
        HttpResponseWrapper resp = http.getWithParams("https://api.example.com/search",
                Map.of("q", "example", "limit", "10"));

        // GET with headers
        HttpResponseWrapper resp2 = http.get("https://api.example.com/data",
                Map.of("User-Agent", "YaciPlugin/1.0", "Accept", "application/json"));

    }

    public void http4() {
        PluginHttpClient http = context().http();

        HttpResponseWrapper response = http.get("https://api.example.com/users");
        if (response.isSuccess()) {
            Map<String, Object> users = response.asJsonMap();
            System.out.println("Found " + users.size() + " users");
        }

        // GET with query parameters
        HttpResponseWrapper resp = http.getWithParams("https://api.example.com/search",
                Map.of("q", "example", "limit", "10"));

        // GET with headers
        HttpResponseWrapper resp2 = http.get("https://api.example.com/data",
                Map.of("User-Agent", "YaciPlugin/1.0", "Accept", "application/json"));

    }

    public void http5() {
        // Java - Parse JSON objects and arrays (http() returns a PluginHttpClient)
        PluginHttpClient http = context().http();
        HttpResponseWrapper response = http.get("https://api.example.com/blocks/latest");

        if (response.isJson()) {
            // For a JSON object — asJson() / asJsonMap() return Map<String, Object>
            Map<String, Object> blockData = response.asJsonMap();
            Object blockNumber = blockData.get("number");
            Object blockHash = blockData.get("hash");
            System.out.println("Latest block: " + blockNumber + " (" + blockHash + ")");

            // For a JSON array — asJsonList() returns List<Object>
            HttpResponseWrapper txResp = http.get("https://api.example.com/transactions");
            if (txResp.isJson()) {
                List<Object> transactions = txResp.asJsonList();
                System.out.println("Found " + transactions.size() + " transactions");
                for (Object tx : transactions) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> txMap = (Map<String, Object>) tx;
                    System.out.println("TX: " + txMap.get("hash") + ", Amount: " + txMap.get("amount"));
                }
            }
        } else {
            System.out.println("Unexpected content type: " + response.getContentType());
        }

    }

    @Override
    public void handleEvent(Object event) {
        if (event instanceof BlockEvent blockEvent) {

        } else {
            log.warn("Not a transaction event");
        }
    }

}
