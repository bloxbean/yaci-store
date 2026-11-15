package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import com.bloxbean.cardano.yaci.store.snapshot.archive.Checksums;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ed25519 signatures over a snapshot's {@code SHA256SUMS} (ADR 0005, D9).
 *
 * <p>{@code SHA256SUMS} lists the manifest's and every part's SHA-256, and the manifest pins every
 * file's, so one signature covers the whole snapshot. The signature is the raw 64 bytes in
 * {@code SHA256SUMS.sig}, verifiable without Yaci Store:
 * {@code openssl pkeyutl -verify -pubin -inkey key.pub -rawin -in SHA256SUMS -sigfile SHA256SUMS.sig}.
 * Keys are standard PEM (PKCS#8 private, X.509 public), as {@code openssl genpkey -algorithm ed25519}
 * writes them.
 */
public final class SnapshotSignature {

    public static final String ALGORITHM = "Ed25519";
    public static final String SIGNATURE = "SHA256SUMS.sig";
    public static final String SIGNATURE_INFO = "SHA256SUMS.sig.json";

    private SnapshotSignature() {
    }

    // ------------------------------------------------------------------ keys

    public record KeyFiles(Path privateKey, Path publicKey, String keyId) {}

    /** Writes a new key pair: {@code <name>.pem} (owner-only where the file system allows) and {@code <name>.pub}. */
    public static KeyFiles generate(Path dir, String name) throws IOException {
        Path priv = dir.resolve(name + ".pem");
        Path pub = dir.resolve(name + ".pub");
        if (Files.exists(priv) || Files.exists(pub)) {
            throw new IOException("Refusing to overwrite an existing key in " + dir);
        }
        KeyPair pair;
        try {
            pair = KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        Files.createDirectories(dir);
        Files.writeString(priv, pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        try {
            Files.setPosixFilePermissions(priv, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // not a POSIX file system
        }
        Files.writeString(pub, pem("PUBLIC KEY", pair.getPublic().getEncoded()));
        return new KeyFiles(priv, pub, keyId(pair.getPublic()));
    }

    public static PrivateKey readPrivateKey(Path pem) throws IOException {
        try {
            return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(der(pem, "PRIVATE KEY")));
        } catch (GeneralSecurityException e) {
            throw new IOException(pem + " is not an Ed25519 private key in PKCS#8 PEM", e);
        }
    }

    public static PublicKey readPublicKey(Path pem) throws IOException {
        try {
            return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(der(pem, "PUBLIC KEY")));
        } catch (GeneralSecurityException e) {
            throw new IOException(pem + " is not an Ed25519 public key in X.509 PEM", e);
        }
    }

    /** The first 16 hex digits of the SHA-256 of the encoded public key. */
    public static String keyId(PublicKey key) {
        return Digests.sha256Hex(key.getEncoded()).substring(0, 16);
    }

    // ------------------------------------------------------------------ sign

    /**
     * Signs the {@code SHA256SUMS} next to {@code manifestPath}, after checking it lists this manifest
     * and its parts, and writes {@code SHA256SUMS.sig} and {@code SHA256SUMS.sig.json}.
     *
     * @param publicKey the private key's public half: it names the key, and the signature is checked
     *                  with it before anything is written
     */
    public static String sign(Path manifestPath, PrivateKey privateKey, PublicKey publicKey) throws IOException {
        Path dir = manifestPath.toAbsolutePath().getParent();
        List<String> problems = checksumProblems(manifestPath);
        if (!problems.isEmpty()) {
            throw new IOException("Refusing to sign: " + String.join("; ", problems));
        }
        byte[] sums = Files.readAllBytes(dir.resolve(Checksums.FILE));
        byte[] signature;
        try {
            Signature s = Signature.getInstance(ALGORITHM);
            s.initSign(privateKey);
            s.update(sums);
            signature = s.sign();
        } catch (GeneralSecurityException e) {
            throw new IOException("Signing failed: " + e.getMessage(), e);
        }
        if (!verifies(sums, signature, publicKey)) {
            throw new IOException("The public key does not belong to the signing key");
        }
        Files.write(dir.resolve(SIGNATURE), signature);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("algorithm", ALGORITHM);
        info.put("keyId", keyId(publicKey));
        info.put("publicKey", Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        info.put("signedFile", Checksums.FILE);
        info.put("signedAt", Instant.now().toString());
        Files.writeString(dir.resolve(SIGNATURE_INFO),
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(info) + "\n");
        return keyId(publicKey);
    }

    // ------------------------------------------------------------------ verify

    public enum Status {
        /** Signed by one of the trusted keys, and SHA256SUMS matches the manifest. */
        VERIFIED,
        /** No signature. */
        UNSIGNED,
        /** Signed, but no trusted key is configured to check it with. */
        UNVERIFIED,
        /** The signature or the checksums are wrong: the snapshot is not what was signed. */
        INVALID
    }

    public record Verification(Status status, String keyId, List<String> problems) {
        public boolean trusted() {
            return status == Status.VERIFIED;
        }

        public String describe() {
            return switch (status) {
                case VERIFIED -> "signed by trusted key " + keyId;
                case UNSIGNED -> "not signed";
                case UNVERIFIED -> "signed by key " + keyId + ", but no trusted key is configured to verify it";
                case INVALID -> "signature verification failed: " + String.join("; ", problems);
            };
        }
    }

    /** Checks the signature next to {@code manifestPath} against {@code trusted}. */
    public static Verification verify(Path manifestPath, List<PublicKey> trusted) throws IOException {
        Path dir = manifestPath.toAbsolutePath().getParent();
        Path sigPath = dir.resolve(SIGNATURE);
        String claimedKey = claimedKeyId(dir.resolve(SIGNATURE_INFO));
        if (!Files.isRegularFile(sigPath)) {
            return new Verification(Status.UNSIGNED, null, List.of());
        }
        List<String> problems = new ArrayList<>(checksumProblems(manifestPath));
        if (trusted == null || trusted.isEmpty()) {
            return problems.isEmpty() ? new Verification(Status.UNVERIFIED, claimedKey, List.of())
                    : new Verification(Status.INVALID, claimedKey, problems);
        }
        byte[] sums = Files.readAllBytes(dir.resolve(Checksums.FILE));
        byte[] signature = Files.readAllBytes(sigPath);
        PublicKey signer = trusted.stream().filter(k -> verifies(sums, signature, k)).findFirst().orElse(null);
        if (signer == null) {
            problems.add(0, "the signature does not verify with any trusted key"
                    + (claimedKey == null ? "" : " (it names key " + claimedKey + ")"));
        }
        if (!problems.isEmpty()) {
            return new Verification(Status.INVALID, claimedKey, problems);
        }
        return new Verification(Status.VERIFIED, keyId(signer), List.of());
    }

    /** SHA256SUMS must list exactly this manifest's digest and its parts' digests. */
    static List<String> checksumProblems(Path manifestPath) throws IOException {
        Path dir = manifestPath.toAbsolutePath().getParent();
        Path sumsPath = dir.resolve(Checksums.FILE);
        if (!Files.isRegularFile(sumsPath)) {
            return List.of(Checksums.FILE + " is missing");
        }
        Map<String, String> sums;
        try {
            sums = Checksums.parse(Files.readString(sumsPath, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return List.of(e.getMessage());
        }
        List<String> problems = new ArrayList<>();
        String manifestName = manifestPath.getFileName().toString();
        String manifestSha = Digests.sha256Hex(manifestPath);
        if (!manifestSha.equals(sums.get(manifestName))) {
            problems.add(Checksums.FILE + " does not list this manifest's SHA-256 for " + manifestName);
        }
        SnapshotManifest manifest = new ManifestCodec().read(manifestPath);
        for (SnapshotManifest.PartManifest p : manifest.parts()) {
            if (!p.sha256().equals(sums.get(p.fileName()))) {
                problems.add(Checksums.FILE + " and the manifest disagree on " + p.fileName());
            }
        }
        if (sums.size() != manifest.parts().size() + 1) {
            problems.add(Checksums.FILE + " lists " + sums.size() + " files; the snapshot has " + (manifest.parts().size() + 1));
        }
        return problems;
    }

    private static boolean verifies(byte[] data, byte[] signature, PublicKey key) {
        try {
            Signature s = Signature.getInstance(ALGORITHM);
            s.initVerify(key);
            s.update(data);
            return s.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static String claimedKeyId(Path info) {
        try {
            if (!Files.isRegularFile(info)) {
                return null;
            }
            var node = new ObjectMapper().readTree(info.toFile());
            return node.hasNonNull("keyId") ? node.get("keyId").asText() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n" + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(der) + "\n-----END " + type + "-----\n";
    }

    private static byte[] der(Path pem, String type) throws IOException {
        String text = Files.readString(pem, StandardCharsets.US_ASCII);
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        int b = text.indexOf(begin);
        int e = text.indexOf(end);
        if (b < 0 || e < b) {
            throw new IOException(pem + " has no " + type + " block");
        }
        return Base64.getMimeDecoder().decode(text.substring(b + begin.length(), e));
    }
}
