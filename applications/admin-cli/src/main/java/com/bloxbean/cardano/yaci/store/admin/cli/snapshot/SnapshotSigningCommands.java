package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.admin.cli.Groups;
import com.bloxbean.cardano.yaci.store.admin.cli.common.CommandFailedException;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.nio.file.Path;

import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.info;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.success;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.writeLn;

/** Ed25519 keys and signatures for snapshots (ADR 0005, D9). */
@Command(group = Groups.SNAPSHOT_CMD_GROUP)
@RequiredArgsConstructor
public class SnapshotSigningCommands {

    private final SnapshotCliSupport support;

    @Command(command = "snapshot keygen", description = "Create an Ed25519 key pair for signing snapshots.")
    public void keygen(
            @Option(longNames = "output", required = true, description = "Directory to write the key pair into")
            String output,
            @Option(longNames = "name", defaultValue = "snapshot-signing", description = "File name, without extension")
            String name) {
        try {
            var keys = SnapshotSignature.generate(Path.of(output).toAbsolutePath().normalize(), name);
            writeLn(success("Key %s", keys.keyId()));
            writeLn("  Private key : %s (keep it secret; set store.snapshot.signing-key to it)", keys.privateKey());
            writeLn("  Public key  : %s (publish it; importers set store.snapshot.trusted-keys to it)", keys.publicKey());
        } catch (Exception e) {
            throw SnapshotCommands.failed("snapshot keygen", e);
        }
    }

    @Command(command = "snapshot sign", description = "Sign an exported snapshot's SHA256SUMS.")
    public void sign(
            @Option(longNames = "manifest", required = true, description = "Manifest of the exported snapshot")
            String manifest,
            @Option(longNames = "signing-key",
                    description = "Ed25519 private key (PKCS#8 PEM) (default: store.snapshot.signing-key)")
            String signingKey) {
        try {
            var key = support.signingKey(signingKey);
            if (key == null) {
                throw new CommandFailedException("snapshot sign: pass --signing-key or set store.snapshot.signing-key", null);
            }
            Path m = Path.of(manifest).toAbsolutePath().normalize();
            String keyId = SnapshotSignature.sign(m, key.privateKey(), key.publicKey());
            writeLn(success("Signed %s with key %s", m.getParent().resolve("SHA256SUMS"), keyId));
            writeLn(info("Publish it again ('snapshot publish') so the store has the signature."));
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw SnapshotCommands.failed("snapshot sign", e);
        }
    }
}
