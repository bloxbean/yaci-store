package com.bloxbean.cardano.yaci.store.submit.controller.local;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Dev-only local remote signer. Enable with:
 *
 * store.submit.local-remote-signer.enabled=true
 *
 * Exposes POST /local-remote-signer/sign that returns a deterministic signature.
 */
@Configuration
@ConditionalOnProperty(prefix = "store.submit.local-remote-signer", name = "enabled", havingValue = "true")
@Import(LocalRemoteSignerController.class)
public class LocalRemoteSignerConfiguration {
}
