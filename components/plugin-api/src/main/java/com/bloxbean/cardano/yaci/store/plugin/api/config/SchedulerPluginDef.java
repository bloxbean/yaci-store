package com.bloxbean.cardano.yaci.store.plugin.api.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SchedulerPluginDef extends PluginDef {
    private ScheduleConfig schedule;
    private Map<String, Object> customVariables;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleConfig {
        private ScheduleType type;
        private String value;
    }

    public enum ScheduleType {
        INTERVAL,
        CRON
    }
}