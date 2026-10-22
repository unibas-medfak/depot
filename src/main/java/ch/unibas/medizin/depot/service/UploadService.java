package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.config.UploadProperties;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import ch.unibas.medizin.depot.dto.UploadStatusDto;
import ch.unibas.medizin.depot.exception.UploadException;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Resumable uploads: a client starts a session, sends the file as sequential chunks and completes it,
 * upon which the assembled file is stored like a regular {@link DepotService#put put}.
 * <p>
 * Sessions are kept in memory and their data in {@code <base>/tmp/uploads}, which is on the same
 * filesystem as the tenant directories. Sessions do not survive a restart; leftovers are removed on startup.
 */
@Service
@NullMarked
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final DepotService depotService;

    private final UploadProperties uploadProperties;

    private final Path uploadDirectory;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private static final class Session {
        final String id = UUID.randomUUID().toString();
        final String owner;
        final String path;
        final String filename;
        final long size;
        final boolean hash;
        final Path data;
        final ReentrantLock lock = new ReentrantLock();
        long bytesReceived;
        Instant expiresAt;

        Session(String owner, String path, String filename, long size, boolean hash, Path data, Instant expiresAt) {
            this.owner = owner;
            this.path = path;
            this.filename = filename;
            this.size = size;
            this.hash = hash;
            this.data = data;
            this.expiresAt = expiresAt;
        }
    }

    public UploadService(DepotService depotService, DepotProperties depotProperties, UploadProperties uploadProperties) {
        this.depotService = depotService;
        this.uploadProperties = uploadProperties;
        this.uploadDirectory = depotProperties.getBaseDirectory().resolve("tmp").resolve("uploads");
    }

    @PostConstruct
    private void init() {
        try {
            FileSystemUtils.deleteRecursively(uploadDirectory);
            Files.createDirectories(uploadDirectory);
        } catch (IOException e) {
            log.error("Could not initialize upload directory {}", uploadDirectory, e);
            throw new RuntimeException("Could not initialize upload directory");
        }
    }

    public UploadStatusDto start(final String path, final String filename, final long size, final boolean hash) {
        final var owner = owner();

        if (size > uploadProperties.maxSize().toBytes()) {
            throw new UploadException(HttpStatus.CONTENT_TOO_LARGE, "Upload too large",
                    "size must not exceed " + uploadProperties.maxSize().toBytes() + " bytes", null);
        }

        // Fail early, before the client transfers anything
        depotService.checkPutTarget(path, filename);

        removeExpired();
        if (sessions.values().stream().filter(s -> s.owner.equals(owner)).count() >= uploadProperties.maxSessions()) {
            throw new UploadException(HttpStatus.TOO_MANY_REQUESTS, "Too many uploads",
                    "at most " + uploadProperties.maxSessions() + " uploads may be in progress", null);
        }

        final Path data;
        try {
            data = Files.createFile(uploadDirectory.resolve(UUID.randomUUID() + ".part"));
        } catch (IOException e) {
            log.error("Could not create upload file", e);
            throw new RuntimeException("Could not start the upload.");
        }

        final var session = new Session(owner, path, filename, size, hash, data, Instant.now().plus(uploadProperties.ttl()));
        sessions.put(session.id, session);
        log.info("Started upload {} {} {}", kv("upload", session.id), kv("path", path + "/" + filename), kv("size", size));
        return status(session);
    }

    public UploadStatusDto status(final String id) {
        final var session = find(id);
        return status(session);
    }

    public UploadStatusDto append(final String id, final long offset, final InputStream body, @Nullable final String sha256) {
        final var session = find(id);
        lock(session);
        try {
            if (offset != session.bytesReceived) {
                throw new UploadException(HttpStatus.CONFLICT, "Offset mismatch",
                        "offset must equal the bytes received so far", session.bytesReceived);
            }

            final var chunkSize = uploadProperties.chunkSize().toBytes();
            final var remaining = session.size - session.bytesReceived;
            final var digest = sha256 == null ? null : sha256Digest();

            long written = 0;
            try (var channel = FileChannel.open(session.data, StandardOpenOption.WRITE)) {
                final var buffer = ByteBuffer.allocate(64 * 1024);
                int read;
                while ((read = body.read(buffer.array())) != -1) {
                    written += read;
                    if (written > chunkSize || written > remaining) {
                        channel.truncate(session.bytesReceived);
                        throw new UploadException(HttpStatus.CONTENT_TOO_LARGE, "Chunk too large",
                                "a chunk must not exceed " + Math.min(chunkSize, remaining) + " bytes", session.bytesReceived);
                    }
                    buffer.clear().limit(read);
                    if (digest != null) {
                        digest.update(buffer.array(), 0, read);
                    }
                    var position = session.bytesReceived + written - read;
                    while (buffer.hasRemaining()) {
                        position += channel.write(buffer, position);
                    }
                }

                if (digest != null && !HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(sha256)) {
                    channel.truncate(session.bytesReceived);
                    throw new UploadException(HttpStatus.UNPROCESSABLE_CONTENT, "Checksum mismatch",
                            "the chunk does not match the given checksum", session.bytesReceived);
                }
            } catch (IOException e) {
                // Includes a client that disconnected mid-chunk; drop the partial chunk so the client can resend it
                truncate(session);
                log.info("Could not receive chunk {} {}", kv("upload", id), kv("offset", offset), e);
                throw new UploadException(HttpStatus.BAD_REQUEST, "Chunk not received",
                        "the chunk could not be read completely", session.bytesReceived);
            }

            session.bytesReceived += written;
            session.expiresAt = Instant.now().plus(uploadProperties.ttl());
            return status(session);
        } finally {
            session.lock.unlock();
        }
    }

    public PutFileResponseDto complete(final String id) {
        final var session = find(id);
        lock(session);
        try {
            if (session.bytesReceived != session.size) {
                throw new UploadException(HttpStatus.CONFLICT, "Upload incomplete",
                        "received " + session.bytesReceived + " of " + session.size + " bytes", session.bytesReceived);
            }

            final var response = depotService.putFromFile(session.data, session.path, session.filename, session.hash);
            sessions.remove(session.id);
            log.info("Completed upload {} {}", kv("upload", id), kv("bytes", response.bytes()));
            return response;
        } finally {
            session.lock.unlock();
        }
    }

    public void abort(final String id) {
        final var session = find(id);
        lock(session);
        try {
            discard(session);
            log.info("Aborted upload {}", kv("upload", id));
        } finally {
            session.lock.unlock();
        }
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT10M")
    void removeExpired() {
        final var now = Instant.now();
        for (var session : sessions.values()) {
            if (session.expiresAt.isBefore(now) && session.lock.tryLock()) {
                try {
                    discard(session);
                    log.info("Removed expired upload {}", kv("upload", session.id));
                } finally {
                    session.lock.unlock();
                }
            }
        }
    }

    private Session find(final String id) {
        final var session = sessions.get(id);
        // Another subject's upload is reported as unknown so IDs cannot be probed
        if (session == null || !session.owner.equals(owner()) || session.expiresAt.isBefore(Instant.now())) {
            throw new UploadException(HttpStatus.NOT_FOUND, "Upload not found", "unknown or expired upload", null);
        }
        return session;
    }

    private void lock(final Session session) {
        if (!session.lock.tryLock()) {
            throw new UploadException(HttpStatus.CONFLICT, "Upload busy", "another request for this upload is in progress", null);
        }
        // Removed (completed, aborted or expired) between finding the session and locking it
        if (!sessions.containsKey(session.id)) {
            session.lock.unlock();
            throw new UploadException(HttpStatus.NOT_FOUND, "Upload not found", "unknown or expired upload", null);
        }
    }

    private void discard(final Session session) {
        sessions.remove(session.id);
        try {
            Files.deleteIfExists(session.data);
        } catch (IOException e) {
            log.error("Could not delete upload file {}", kv("path", session.data.toString()), e);
        }
    }

    private static void truncate(final Session session) {
        try (var channel = FileChannel.open(session.data, StandardOpenOption.WRITE)) {
            channel.truncate(session.bytesReceived);
        } catch (IOException e) {
            log.error("Could not truncate upload file {}", kv("path", session.data.toString()), e);
        }
    }

    private UploadStatusDto status(final Session session) {
        return new UploadStatusDto(session.id, session.size, session.bytesReceived, uploadProperties.chunkSize().toBytes(), session.expiresAt);
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String owner() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Not authenticated");
        }
        return authentication.getName();
    }

}
