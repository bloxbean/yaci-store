package com.bloxbean.cardano.yaci.store.submit.dev;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Dev-only stub remote signer. Enable with:
 *
 * store.submit.stub-remote-signer.enabled=true
 *
 * Exposes POST /stub-remote-signer/sign that returns a deterministic fake signature.
 */
@Configuration
@ConditionalOnProperty(prefix = "store.submit.stub-remote-signer", name = "enabled", havingValue = "true")
@Import(StubRemoteSignerController.class)
public class StubRemoteSignerConfiguration {
}
