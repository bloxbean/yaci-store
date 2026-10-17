package com.bloxbean.cardano.yaci.store.snapshot.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.LongConsumer;

/**
 * Any S3-compatible bucket: Cloudflare R2, AWS S3, MinIO. Nothing here is vendor-specific beyond
 * the endpoint, region and addressing style, which come from configuration.
 *
 * <p>Large files are uploaded in pieces, several at once, each read straight from the file (nothing
 * is buffered in memory, and a retried piece is re-read). Integrity is the snapshot's own SHA-256,
 * recorded as {@code x-amz-meta-sha256}; request checksums are only sent where S3 requires them,
 * because S3-compatible services do not all accept the SDK's newer default checksums.
 */
public class S3SnapshotStore implements SnapshotStore {

    private static final Logger log = LoggerFactory.getLogger(S3SnapshotStore.class);
    private static final String SHA256 = "sha256";

    /**
     * @param endpoint     the S3 API endpoint, e.g. {@code https://<account>.r2.cloudflarestorage.com};
     *                     null for AWS S3
     * @param region       {@code auto} for R2; any value for MinIO
     * @param pathStyle    {@code bucket} in the path rather than the host name; MinIO needs it
     * @param accessKeyId  null to use the AWS default credential chain (environment, profile, ...)
     * @param pieceBytes   size of each piece of a multipart upload; files up to this size go in one PUT
     * @param uploadThreads pieces uploaded at once per file
     */
    public record Settings(String endpoint, String region, boolean pathStyle, String accessKeyId,
                           String secretAccessKey, long pieceBytes, int uploadThreads) {
        public static final long DEFAULT_PIECE_BYTES = 64L << 20;
        public static final int DEFAULT_UPLOAD_THREADS = 4;
        /** S3's smallest multipart piece other than the last. */
        public static final long MIN_PIECE_BYTES = 5L << 20;

        public Settings {
            if (pieceBytes == 0) {
                pieceBytes = DEFAULT_PIECE_BYTES;
            }
            if (pieceBytes < MIN_PIECE_BYTES) {
                throw new IllegalArgumentException("store.snapshot.remote.piece-size must be at least 5MiB");
            }
            uploadThreads = Math.max(1, uploadThreads);
        }

        @Override
        public String toString() {
            // Never print the secret, even by accident.
            return "Settings[endpoint=" + endpoint + ", region=" + region + ", pathStyle=" + pathStyle
                    + ", credentials=" + (accessKeyId == null || accessKeyId.isBlank() ? "default chain" : "configured") + "]";
        }
    }

    private final S3Client s3;
    private final String bucket;
    private final String prefix;
    private final Settings settings;

    /** @param prefix key prefix inside the bucket, without leading or trailing slashes; may be empty */
    public S3SnapshotStore(String bucket, String prefix, Settings settings) {
        this.bucket = bucket;
        this.prefix = prefix == null || prefix.isBlank() ? "" : prefix.replaceAll("^/+|/+$", "") + "/";
        this.settings = settings;
        AwsCredentialsProvider credentials = settings.accessKeyId() == null || settings.accessKeyId().isBlank()
                ? DefaultCredentialsProvider.builder().build()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(settings.accessKeyId(), settings.secretAccessKey()));
        var builder = S3Client.builder()
                .httpClient(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(30))
                        .socketTimeout(Duration.ofMinutes(5))
                        .build())
                .region(Region.of(settings.region() == null || settings.region().isBlank() ? "auto" : settings.region()))
                .credentialsProvider(credentials)
                .forcePathStyle(settings.pathStyle())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        if (settings.endpoint() != null && !settings.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(settings.endpoint()));
        }
        this.s3 = builder.build();
    }

    @Override
    public String describe() {
        return "s3://" + bucket + "/" + prefix + (settings.endpoint() == null || settings.endpoint().isBlank()
                ? "" : " at " + settings.endpoint());
    }

    @Override
    public boolean writable() {
        return true;
    }

    private String key(String k) {
        return prefix + k;
    }

    @Override
    public Optional<ObjectInfo> head(String k) throws IOException {
        try {
            HeadObjectResponse r = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key(k)).build());
            return Optional.of(new ObjectInfo(r.contentLength(), r.eTag(), r.metadata().get(SHA256)));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw io("HEAD " + k, e);
        } catch (SdkException e) {
            throw io("HEAD " + k, e);
        }
    }

    @Override
    public InputStream open(String k) throws IOException {
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key(k)).build());
        } catch (NoSuchKeyException e) {
            throw new NoSuchFileException(k + " is not in " + describe());
        } catch (SdkException e) {
            throw io("GET " + k, e);
        }
    }

    @Override
    public Optional<Tagged> read(String k) throws IOException {
        try {
            var r = s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key(k)).build());
            return Optional.of(new Tagged(r.asByteArray(), r.response().eTag()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            throw io("GET " + k, e);
        }
    }

    @Override
    public List<String> list(String p) throws IOException {
        try {
            List<String> keys = new ArrayList<>();
            s3.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucket).prefix(key(p)).build())
                    .contents().forEach(o -> keys.add(o.key().substring(prefix.length())));
            keys.sort(Comparator.naturalOrder());
            return keys;
        } catch (SdkException e) {
            throw io("LIST " + p, e);
        }
    }

    @Override
    public void put(String k, Path file, String sha256, LongConsumer sent) throws IOException {
        long size = java.nio.file.Files.size(file);
        Map<String, String> metadata = sha256 == null ? Map.of() : Map.of(SHA256, sha256);
        try {
            if (size <= settings.pieceBytes()) {
                s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key(k)).metadata(metadata).build(),
                        RequestBody.fromFile(file));
                if (sent != null) {
                    sent.accept(size);
                }
                return;
            }
            multipart(k, file, size, metadata, sent);
        } catch (SdkException e) {
            throw io("PUT " + k, e);
        }
    }

    private void multipart(String k, Path file, long size, Map<String, String> metadata, LongConsumer sent)
            throws IOException {
        String uploadId = s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(bucket).key(key(k)).metadata(metadata).build()).uploadId();
        long piece = settings.pieceBytes();
        int pieces = (int) ((size + piece - 1) / piece);
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(settings.uploadThreads(), pieces)));
        boolean completed = false;
        try {
            List<Future<CompletedPart>> futures = new ArrayList<>();
            for (int i = 0; i < pieces; i++) {
                int number = i + 1;
                long offset = (long) i * piece;
                long length = Math.min(piece, size - offset);
                futures.add(pool.submit(() -> {
                    // A provider rather than a stream: a retried request reads the piece again.
                    RequestBody body = RequestBody.fromContentProvider(() -> slice(file, offset, length), length,
                            "application/octet-stream");
                    String etag = s3.uploadPart(UploadPartRequest.builder().bucket(bucket).key(key(k))
                            .uploadId(uploadId).partNumber(number).contentLength(length).build(), body).eTag();
                    if (sent != null) {
                        sent.accept(length);
                    }
                    return CompletedPart.builder().partNumber(number).eTag(etag).build();
                }));
            }
            List<CompletedPart> parts = new ArrayList<>();
            for (Future<CompletedPart> f : futures) {
                parts.add(f.get());
            }
            s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder().bucket(bucket).key(key(k))
                    .uploadId(uploadId).multipartUpload(CompletedMultipartUpload.builder().parts(parts).build()).build());
            completed = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while uploading " + k, e);
        } catch (ExecutionException e) {
            Throwable c = e.getCause();
            throw c instanceof IOException io ? io : new IOException("Uploading " + k + " failed: " + c.getMessage(), c);
        } finally {
            pool.shutdownNow();
            if (!completed) {
                try {
                    s3.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket(bucket).key(key(k))
                            .uploadId(uploadId).build());
                } catch (SdkException abort) {
                    log.warn("Could not abort the incomplete upload of {}: {}", k, abort.getMessage());
                }
            }
        }
    }

    private static InputStream slice(Path file, long offset, long length) {
        try {
            FileChannel ch = FileChannel.open(file, StandardOpenOption.READ);
            ch.position(offset);
            InputStream in = Channels.newInputStream(ch);
            return new java.io.FilterInputStream(in) {
                long left = length;

                @Override
                public int read() throws IOException {
                    if (left <= 0) {
                        return -1;
                    }
                    int b = super.read();
                    if (b >= 0) {
                        left--;
                    }
                    return b;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    if (left <= 0) {
                        return -1;
                    }
                    int n = super.read(b, off, (int) Math.min(len, left));
                    if (n > 0) {
                        left -= n;
                    }
                    return n;
                }
            };
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean putIfUnchanged(String k, byte[] bytes, String ifMatch) throws IOException {
        PutObjectRequest.Builder request = PutObjectRequest.builder().bucket(bucket).key(key(k))
                .contentType("application/json");
        if (ifMatch == null) {
            request.ifNoneMatch("*");
        } else {
            request.ifMatch(ifMatch);
        }
        try {
            s3.putObject(request.build(), RequestBody.fromBytes(bytes));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 412 || e.statusCode() == 409) {
                return false;
            }
            if (e.statusCode() == 501 || "NotImplemented".equals(e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode())) {
                log.warn("{} does not support conditional writes; writing {} unconditionally", describe(), k);
                s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key(k)).contentType("application/json").build(),
                        RequestBody.fromBytes(bytes));
                return true;
            }
            throw io("PUT " + k, e);
        } catch (SdkException e) {
            throw io("PUT " + k, e);
        }
    }

    @Override
    public void delete(String k) throws IOException {
        try {
            s3.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                    .bucket(bucket).key(key(k)).build());
        } catch (SdkException e) {
            throw io("DELETE " + k, e);
        }
    }

    @Override
    public void close() {
        s3.close();
    }

    private IOException io(String what, SdkException e) {
        String message = e instanceof S3Exception s && s.awsErrorDetails() != null
                ? s.statusCode() + " " + s.awsErrorDetails().errorCode() + ": " + s.awsErrorDetails().errorMessage()
                : e.getMessage();
        return new IOException(what + " on " + describe() + " failed: " + message, e);
    }
}
