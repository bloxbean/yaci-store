package com.bloxbean.cardano.yaci.store.adminui;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store.admin.ui")
public class AdminUiProperties {
    /**
     * Enable/disable the Admin UI.
     * Default: false (disabled)
     */
    private boolean enabled = false;
}
