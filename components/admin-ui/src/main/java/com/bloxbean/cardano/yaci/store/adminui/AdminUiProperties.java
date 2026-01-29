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

    /**
     * Enable/disable sync control operations (start, stop, restart) in the Admin UI.
     * When disabled, sync control buttons are hidden and API endpoints return 403.
     * Default: false (disabled for safety)
     */
    private boolean syncControlEnabled = false;

    /**
     * Header text displayed in the Admin UI.
     * Can be customized for white-labeling purposes.
     * Default: "Yaci Store Admin (Beta)"
     */
    private String headerText = "Yaci Store Admin (Beta)";

    /**
     * Enable/disable Koios ledger state verification in the Admin UI.
     * When disabled, the "Verify with Koios" button is hidden on the Ledger State page.
     * Useful for deployments behind firewalls where the backend cannot reach Koios servers.
     * Default: true (enabled)
     */
    private boolean koiosVerificationEnabled = true;
}
