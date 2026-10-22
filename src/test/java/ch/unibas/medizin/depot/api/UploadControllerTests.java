package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import ch.unibas.medizin.depot.dto.UploadStartRequestDto;
import ch.unibas.medizin.depot.dto.UploadStatusDto;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"depot.upload.chunk-size=1KB", "depot.upload.max-size=1MB", "depot.upload.max-sessions=3"})
public class UploadControllerTests {

    private static final int CHUNK = 1024;

    @Autowired
    private DepotProperties depotProperties;

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    private String token;

    private String subject;

    @BeforeEach
    void setUp() throws IOException {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("tenant_a").resolve("upload_realm").toFile());
        // Open sessions are limited per subject and outlive a test, so every test gets its own
        subject = "subject_" + UUID.randomUUID();
        token = token("upload_realm", subject, "rwd");
    }

    @Test
    public void Upload_file_in_chunks_and_store_it() throws IOException {
        var data = randomBytes(2 * CHUNK + 100);
        var upload = start("docs", "big.bin", data.length, true);
        assertEquals(CHUNK, upload.chunkSize());

        for (var offset = 0; offset < data.length; offset += CHUNK) {
            var chunk = Arrays.copyOfRange(data, offset, Math.min(offset + CHUNK, data.length));
            var status = chunk(upload.uploadId(), offset, chunk).expectStatus().isOk().expectBody(UploadStatusDto.class).returnResult().getResponseBody();
            assertEquals(offset + chunk.length, Objects.requireNonNull(status).bytesReceived());
        }

        var response = complete(upload.uploadId()).expectStatus().isOk().expectBody(PutFileResponseDto.class).returnResult().getResponseBody();
        assertNotNull(response);
        assertEquals(data.length, response.bytes());
        var hash = MurmurHash3.hash128x64(data);
        assertEquals(String.format("%016x%016x", hash[0], hash[1]), response.hash());

        assertArrayEquals(data, Files.readAllBytes(depotProperties.getBaseDirectory().resolve("tenant_a/upload_realm/docs/big.bin")));

        // The session is gone after completion
        webTestClient.get().uri("/upload/" + upload.uploadId()).header("Authorization", "Bearer " + token).exchange().expectStatus().isNotFound();
    }

    @Test
    public void Resume_after_offset_mismatch() {
        var data = randomBytes(2 * CHUNK);
        var upload = start("docs", "resume.bin", data.length, false);

        chunk(upload.uploadId(), 0, Arrays.copyOfRange(data, 0, CHUNK)).expectStatus().isOk();

        // Resending the first chunk or skipping ahead is rejected and reports where to continue
        chunk(upload.uploadId(), 0, Arrays.copyOfRange(data, 0, CHUNK)).expectStatus().isEqualTo(409).expectBody().jsonPath("$.bytesReceived").isEqualTo(CHUNK);
        chunk(upload.uploadId(), 2 * CHUNK, new byte[10]).expectStatus().isEqualTo(409).expectBody().jsonPath("$.bytesReceived").isEqualTo(CHUNK);

        var status = webTestClient.get().uri("/upload/" + upload.uploadId()).header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isOk().expectBody(UploadStatusDto.class).returnResult().getResponseBody();
        assertEquals(CHUNK, Objects.requireNonNull(status).bytesReceived());

        chunk(upload.uploadId(), CHUNK, Arrays.copyOfRange(data, CHUNK, 2 * CHUNK)).expectStatus().isOk();
        complete(upload.uploadId()).expectStatus().isOk();
    }

    @Test
    public void Reject_complete_of_incomplete_upload() {
        var upload = start("docs", "partial.bin", CHUNK + 1, false);
        chunk(upload.uploadId(), 0, new byte[CHUNK]).expectStatus().isOk();

        complete(upload.uploadId()).expectStatus().isEqualTo(409).expectBody().jsonPath("$.bytesReceived").isEqualTo(CHUNK);
        assertFalse(Files.exists(depotProperties.getBaseDirectory().resolve("tenant_a/upload_realm/docs/partial.bin")));
    }

    @Test
    public void Reject_oversized_chunk_and_overshooting_the_declared_size() {
        var upload = start("docs", "oversize.bin", CHUNK + 10, false);
        chunk(upload.uploadId(), 0, new byte[CHUNK + 1]).expectStatus().isEqualTo(413);
        chunk(upload.uploadId(), 0, new byte[CHUNK]).expectStatus().isOk();
        chunk(upload.uploadId(), CHUNK, new byte[11]).expectStatus().isEqualTo(413);
        chunk(upload.uploadId(), CHUNK, new byte[10]).expectStatus().isOk();
        complete(upload.uploadId()).expectStatus().isOk();
    }

    @Test
    public void Reject_chunk_with_wrong_checksum_and_accept_the_right_one() throws Exception {
        var data = randomBytes(CHUNK);
        var upload = start("docs", "checksum.bin", data.length, false);

        chunk(upload.uploadId(), 0, data, "00").expectStatus().isEqualTo(422).expectBody().jsonPath("$.bytesReceived").isEqualTo(0);

        var sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        chunk(upload.uploadId(), 0, data, sha256).expectStatus().isOk();
        complete(upload.uploadId()).expectStatus().isOk();
        assertArrayEquals(data, Files.readAllBytes(depotProperties.getBaseDirectory().resolve("tenant_a/upload_realm/docs/checksum.bin")));
    }

    @Test
    public void Reject_too_large_upload_and_invalid_requests() {
        startRaw(new UploadStartRequestDto("docs", "huge.bin", 1024 * 1024 + 1, false)).expectStatus().isEqualTo(413);
        startRaw(new UploadStartRequestDto("docs", "zero.bin", 0, false)).expectStatus().isBadRequest();
        startRaw(new UploadStartRequestDto("../docs", "a.bin", 10, false)).expectStatus().isBadRequest();
        startRaw(new UploadStartRequestDto("docs", "../a.bin", 10, false)).expectStatus().isBadRequest();
        startRaw(new UploadStartRequestDto(null, "a.bin", 10, false)).expectStatus().isBadRequest();
        startRaw(new UploadStartRequestDto("docs", null, 10, false)).expectStatus().isBadRequest();
    }

    @Test
    public void Reject_start_when_target_is_a_folder() throws IOException {
        Files.createDirectories(depotProperties.getBaseDirectory().resolve("tenant_a/upload_realm/docs/taken"));
        startRaw(new UploadStartRequestDto("docs", "taken", 10, false)).expectStatus().isEqualTo(409);
    }

    @Test
    public void Limit_concurrent_uploads_and_free_slots_on_abort() {
        var first = start("docs", "a.bin", 10, false);
        start("docs", "b.bin", 10, false);
        start("docs", "c.bin", 10, false);
        startRaw(new UploadStartRequestDto("docs", "d.bin", 10, false)).expectStatus().isEqualTo(429);

        webTestClient.delete().uri("/upload/" + first.uploadId()).header("Authorization", "Bearer " + token).exchange().expectStatus().isNoContent();
        webTestClient.get().uri("/upload/" + first.uploadId()).header("Authorization", "Bearer " + token).exchange().expectStatus().isNotFound();
        start("docs", "d.bin", 10, false);
    }

    @Test
    public void Hide_upload_from_other_subjects_and_require_write_role() {
        var upload = start("docs", "private.bin", 10, false);

        var otherSubject = token("upload_realm", "someone_else", "rwd");
        webTestClient.get().uri("/upload/" + upload.uploadId()).header("Authorization", "Bearer " + otherSubject).exchange().expectStatus().isNotFound();
        webTestClient.delete().uri("/upload/" + upload.uploadId()).header("Authorization", "Bearer " + otherSubject).exchange().expectStatus().isNotFound();

        var readOnly = token("upload_realm", subject, "r");
        webTestClient.get().uri("/upload/" + upload.uploadId()).header("Authorization", "Bearer " + readOnly).exchange().expectStatus().isForbidden();
        webTestClient.post().uri("/upload").header("Authorization", "Bearer " + readOnly).bodyValue(new UploadStartRequestDto("docs", "x.bin", 10, false))
                .exchange().expectStatus().isForbidden();

        webTestClient.post().uri("/upload").bodyValue(new UploadStartRequestDto("docs", "x.bin", 10, false)).exchange().expectStatus().isUnauthorized();
    }

    private UploadStatusDto start(String path, String filename, long size, boolean hash) {
        var status = startRaw(new UploadStartRequestDto(path, filename, size, hash))
                .expectStatus().isCreated()
                .expectBody(UploadStatusDto.class).returnResult().getResponseBody();
        return Objects.requireNonNull(status);
    }

    private WebTestClient.ResponseSpec startRaw(UploadStartRequestDto request) {
        return webTestClient.post().uri("/upload").header("Authorization", "Bearer " + token).bodyValue(request).exchange();
    }

    private WebTestClient.ResponseSpec chunk(String id, long offset, byte[] bytes) {
        return chunk(id, offset, bytes, null);
    }

    private WebTestClient.ResponseSpec chunk(String id, long offset, byte[] bytes, String checksum) {
        var request = webTestClient.put().uri("/upload/" + id + "?offset=" + offset)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);
        if (checksum != null) {
            request = request.header("X-Chunk-Checksum", checksum);
        }
        return request.bodyValue(bytes).exchange();
    }

    private WebTestClient.ResponseSpec complete(String id) {
        return webTestClient.post().uri("/upload/" + id + "/complete").header("Authorization", "Bearer " + token).exchange();
    }

    private String token(String realm, String subject, String mode) {
        return JWT.create()
                .withIssuer("depot")
                .withClaim("tenant", "tenant_a")
                .withClaim("realm", realm)
                .withClaim("mode", mode)
                .withSubject(subject)
                .withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .sign(Algorithm.HMAC256(depotProperties.getJwtSecret()));
    }

    private static byte[] randomBytes(int size) {
        var bytes = new byte[size];
        new Random().nextBytes(bytes);
        return bytes;
    }

}
