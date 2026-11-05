package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import com.bloxbean.cardano.yaci.store.snapshot.remote.SnapshotRepositoryTest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotSignatureTest {

    @TempDir
    Path tmp;

    private static final String ID = "55555555-5555-5555-5555-555555555555";

    @Test
    void aSignedSnapshotVerifiesWithItsKeyAndNoOther() throws Exception {
        Path manifest = SnapshotRepositoryTest.snapshot(tmp.resolve("s"), ID, 100, null);
        var keys = SnapshotSignature.generate(tmp.resolve("keys"), "publisher");
        var other = SnapshotSignature.generate(tmp.resolve("keys"), "other");
        var pub = SnapshotSignature.readPublicKey(keys.publicKey());

        assertThat(SnapshotSignature.verify(manifest, List.of(pub)).status()).isEqualTo(SnapshotSignature.Status.UNSIGNED);
        String keyId = SnapshotSignature.sign(manifest, SnapshotSignature.readPrivateKey(keys.privateKey()), pub);

        assertThat(keyId).isEqualTo(keys.keyId()).hasSize(16);
        var verified = SnapshotSignature.verify(manifest, List.of(SnapshotSignature.readPublicKey(other.publicKey()), pub));
        assertThat(verified.status()).isEqualTo(SnapshotSignature.Status.VERIFIED);
        assertThat(verified.keyId()).isEqualTo(keyId);
        assertThat(SnapshotSignature.verify(manifest, List.of()).status()).isEqualTo(SnapshotSignature.Status.UNVERIFIED);
        var wrongKey = SnapshotSignature.verify(manifest, List.of(SnapshotSignature.readPublicKey(other.publicKey())));
        assertThat(wrongKey.status()).isEqualTo(SnapshotSignature.Status.INVALID);
        assertThat(wrongKey.describe()).contains("does not verify with any trusted key").contains(keyId);
        assertThat(Files.readString(manifest.resolveSibling("SHA256SUMS.sig.json"))).contains(keyId).doesNotContain("PRIVATE");
    }

    @Test
    void aChangedManifestOrChecksumFileFailsVerification() throws Exception {
        Path manifest = SnapshotRepositoryTest.snapshot(tmp.resolve("s"), ID, 100, null);
        var keys = SnapshotSignature.generate(tmp.resolve("keys"), "publisher");
        var pub = SnapshotSignature.readPublicKey(keys.publicKey());
        SnapshotSignature.sign(manifest, SnapshotSignature.readPrivateKey(keys.privateKey()), pub);

        Files.writeString(manifest, Files.readString(manifest).replace("\"test\"", "\"evil\""));
        var changedManifest = SnapshotSignature.verify(manifest, List.of(pub));
        assertThat(changedManifest.status()).isEqualTo(SnapshotSignature.Status.INVALID);
        assertThat(changedManifest.problems()).anyMatch(p -> p.contains("does not list this manifest"));

        Path sums = manifest.resolveSibling("SHA256SUMS");
        // A blank line changes nothing the parser reads, but the signed bytes differ.
        Files.writeString(sums, Files.readString(sums) + "\n");
        assertThat(SnapshotSignature.verify(manifest, List.of(pub)).problems())
                .anyMatch(p -> p.contains("does not verify with any trusted key"));
    }

    @Test
    void refusesToSignChecksumsThatDoNotMatchTheManifestOrWithAMismatchedPublicKey() throws Exception {
        Path manifest = SnapshotRepositoryTest.snapshot(tmp.resolve("s"), ID, 100, null);
        var keys = SnapshotSignature.generate(tmp.resolve("keys"), "publisher");
        var other = SnapshotSignature.generate(tmp.resolve("keys"), "other");
        var priv = SnapshotSignature.readPrivateKey(keys.privateKey());

        assertThatThrownBy(() -> SnapshotSignature.sign(manifest, priv, SnapshotSignature.readPublicKey(other.publicKey())))
                .hasMessageContaining("does not belong");
        Files.writeString(manifest.resolveSibling("SHA256SUMS"), "");
        assertThatThrownBy(() -> SnapshotSignature.sign(manifest, priv, SnapshotSignature.readPublicKey(keys.publicKey())))
                .hasMessageContaining("Refusing to sign");
        assertThatThrownBy(() -> SnapshotSignature.generate(tmp.resolve("keys"), "publisher"))
                .hasMessageContaining("overwrite");
    }

    @Test
    void interoperatesWithOpenssl() throws Exception {
        String openssl = openssl3();
        Assumptions.assumeTrue(openssl != null, "no OpenSSL 3 binary");
        Path manifest = SnapshotRepositoryTest.snapshot(tmp.resolve("s"), ID, 100, null);
        Path priv = tmp.resolve("ossl.pem");
        Path pub = tmp.resolve("ossl.pub");
        run(openssl, "genpkey", "-algorithm", "ed25519", "-out", priv.toString());
        run(openssl, "pkey", "-in", priv.toString(), "-pubout", "-out", pub.toString());

        SnapshotSignature.sign(manifest, SnapshotSignature.readPrivateKey(priv), SnapshotSignature.readPublicKey(pub));

        assertThat(run(openssl, "pkeyutl", "-verify", "-pubin", "-inkey", pub.toString(), "-rawin",
                "-in", manifest.resolveSibling("SHA256SUMS").toString(),
                "-sigfile", manifest.resolveSibling("SHA256SUMS.sig").toString())).contains("Signature Verified Successfully");
        var keys = SnapshotSignature.generate(tmp.resolve("keys"), "java");
        assertThat(run(openssl, "pkey", "-in", keys.privateKey().toString(), "-pubout"))
                .isEqualTo(Files.readString(keys.publicKey()));
    }

    private static String openssl3() {
        for (String candidate : List.of("/opt/homebrew/bin/openssl", "/usr/local/bin/openssl", "/usr/bin/openssl")) {
            try {
                if (Files.isExecutable(Path.of(candidate)) && run(candidate, "version").startsWith("OpenSSL 3")) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // try the next one
            }
        }
        return null;
    }

    private static String run(String... command) throws Exception {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        assertThat(p.waitFor(30, TimeUnit.SECONDS)).isTrue();
        assertThat(p.exitValue()).withFailMessage(String.join(" ", command) + ": " + out).isZero();
        return out;
    }
}
