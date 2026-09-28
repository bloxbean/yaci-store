package com.bloxbean.example;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.JavaFilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.impl.java.PluginContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bundled-in-app UTXO filter for the {@code utxo.unspent.save} extension point. Keeps only UTXOs
 * whose {@code ownerAddr} is in the configured address set (read from the Spring property
 * {@link #ADDRESSES_PROPERTY} via the {@link PluginContext}). With no addresses configured it is a
 * no-op (keeps everything).
 *
 * <p>Lives on this app's own classpath, so {@code JavaStorePluginFactory} resolves it via
 * {@code Class.forName("com.bloxbean.example.AddressFilterPlugin")} — no ext-jar needed.</p>
 */
@Slf4j
public class AddressFilterPlugin extends JavaFilterPlugin<AddressUtxo> {

    public static final String ADDRESSES_PROPERTY = "plugin.address-filter.addresses";

    private final Set<String> addresses;

    public AddressFilterPlugin(PluginDef def, PluginType type, PluginContext ctx) {
        super(def, type, ctx);
        this.addresses = parseAddresses(ctx.environment().getProperty(ADDRESSES_PROPERTY));
        log.info("AddressFilterPlugin '{}' tracking {} address(es): {}", def.getName(), addresses.size(), addresses);
    }

    @Override
    public Collection<AddressUtxo> filter(Collection<AddressUtxo> items) {
        if (addresses.isEmpty()) {
            log.debug("No addresses configured for '{}'; keeping all {} item(s)", getName(), items == null ? 0 : items.size());
            return items;
        }
        int incoming = items == null ? 0 : items.size();
        var kept = items == null ? java.util.List.<AddressUtxo>of() : items.stream()
                .filter(u -> u.getOwnerAddr() != null && addresses.contains(u.getOwnerAddr()))
                .collect(Collectors.toList());
        log.debug("Filter '{}' kept {}/{} UTXO(s)", getName(), kept.size(), incoming);
        return kept;
    }

    private static Set<String> parseAddresses(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split("[\\s,]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
