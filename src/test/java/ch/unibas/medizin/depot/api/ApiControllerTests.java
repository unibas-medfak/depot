package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiControllerTests {

    private final LocalDate today = LocalDate.now(ZoneId.systemDefault());

    private final LocalDate tomorrow = LocalDate.now(ZoneId.systemDefault()).plusDays(1);

    @Autowired
    private DepotProperties depotProperties;

    @LocalServerPort
    private int port;

    private WebTestClient  webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).responseTimeout(Duration.ofMinutes(1)).build();
    }

    @Test
    public void Register_client_with_valid_request() {
        var validRegisterRequest = new AccessTokenRequestDto("tenant_b", "tenant_b_secret", "re_al-m1", "Sub Ject 01.01.2099", "r", tomorrow);
        var validRegisterResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(validRegisterRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(validRegisterResponse);
        assertNotNull(validRegisterResponse.token());
    }

    @Test
    public void Request_token() {
        var validRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "re_al-m1", "subject1", "r", LocalDate.of(2037, 11, 13));
        var validRegisterResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(validRegisterRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(validRegisterResponse);
        var referenceToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkZXBvdCIsInRlbmFudCI6InRlbmFudF9hIiwicmVhbG0iOiJyZV9hbC1tMSIsIm1vZGUiOiJyIiwic3ViIjoic3ViamVjdDEiLCJleHAiOjIxNDE2ODMyMDB9.PHL7p-vOX9ZbmkGZ8aBmnFVPDX00hCHGqt6U3G4Abm8";
        assertEquals(referenceToken, validRegisterResponse.token());
    }

    @Test
    public void Request_token_qr() throws IOException {
        var validRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "re_al-m2", "subject2", "w", LocalDate.of(2050, 12, 31));
        var validRegisterResponse = webTestClient.post()
                .uri("/admin/qr")
                .bodyValue(validRegisterRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(validRegisterResponse);
        // Files.write(new File("qr.png").toPath(), validRegisterResponse);

        var referenceBytes = IOUtils.resourceToByteArray("/qr.png");
        assertArrayEquals(referenceBytes, validRegisterResponse);
    }

    @Test
    public void Deny_client_with_invalid_password() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_a", "wrong_secret", "realm", "subject", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void Deny_client_with_invalid_tenant() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_c", "tenant_a_secret", "realm", "subject", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void Deny_client_with_invalid_tenant_name() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant a", "tenant_a_secret", "realm", "subject", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void Deny_client_with_invalid_realm_name() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm$_", "subject", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void Deny_client_with_invalid_blank_in_realm_name() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "re alm", "subject", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void Deny_client_with_invalid_subject() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", " ", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void Deny_client_with_blank_realm() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "", "subject", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void Deny_client_with_blank_subject() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void Deny_client_with_linebreak_in_realm() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "re\nlm", "subject", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void Deny_client_with_linebreak_in_subject() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "sub\nject", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void Deny_client_with_invalid_admin_password() {
        var invalidRegisterRequest = new AccessTokenRequestDto("tenant_a", "wrong_password", "realm", "subject", "r", tomorrow);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(invalidRegisterRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void Deny_client_with_expiration_date_not_in_the_future() {
        var todayRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "r", today);
        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(todayRegisterRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Disabled
    public void Deny_requests_from_client_with_expiration_date_of_today() {
        var todayRegisterRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "r", today);
        var todayRegisterResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(todayRegisterRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(todayRegisterResponse);
        var token = todayRegisterResponse.token();
        webTestClient.get()
                .uri("/list?path=///test//a")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    public void Deny_file_with_invalid_name() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("tenant_a").resolve("realm").toFile());

        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "rw", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();
        var bytes = new byte[]{1};

        var byteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "/";
            }
        };

        var bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", byteArrayResource);

        webTestClient.post()
                .uri("/put?path=/test&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void Deny_write_file_to_folder_with_same_name() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("tenant_a").resolve("realm").toFile());

        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "rw", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();
        var bytes = new byte[]{1};

        var folderByteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "folder";
            }
        };

        var folderBodyBuilder = new MultipartBodyBuilder();
        folderBodyBuilder.part("file", folderByteArrayResource);

        webTestClient.post()
                .uri("/put?path=/&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(folderBodyBuilder.build()))
                .exchange()
                .expectStatus().isOk();

        var fileByteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "file.txt";
            }
        };

        var fileBodyBuilder = new MultipartBodyBuilder();
        fileBodyBuilder.part("file", fileByteArrayResource);

        webTestClient.post()
                .uri("/put?path=/folder&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(fileBodyBuilder.build()))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void Deny_write_folder_to_file_with_same_name() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("tenant_a").resolve("realm").toFile());

        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "rw", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();
        var bytes = new byte[]{1};

        var fileByteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "file.txt";
            }
        };

        var fileBodyBuilder = new MultipartBodyBuilder();
        fileBodyBuilder.part("file", fileByteArrayResource);

        webTestClient.post()
                .uri("/put?path=/folder&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(fileBodyBuilder.build()))
                .exchange()
                .expectStatus().isOk();

        var folderByteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "folder";
            }
        };

        var folderBodyBuilder = new MultipartBodyBuilder();
        folderBodyBuilder.part("file", folderByteArrayResource);

        webTestClient.post()
                .uri("/put?path=/&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(folderBodyBuilder.build()))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    public void Put_file() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("tenant_a").resolve("realm").toFile());

        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "rw", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();

        var fileSize = 10 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);

        var bodyBuilder1 = new MultipartBodyBuilder();
        bodyBuilder1.part("file", resource);

        var response = webTestClient.post()
                .uri("/put?path=//test/findMe//&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder1.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PutFileResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(fileSize, response.bytes());

        var bodyBuilder2 = new MultipartBodyBuilder();
        bodyBuilder2.part("file", resource);

        var response2 = webTestClient.post()
                .uri("/put?path=//test/findMe/b/&hash=false")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder2.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PutFileResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response2);
        assertEquals(fileSize, response2.bytes());

        var listBody = webTestClient.get()
                .uri("/list?path=///test//findMe")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileDto[].class)
                .returnResult()
                .getResponseBody();
        assertNotNull(listBody);
        assertEquals(2, listBody.length);

        var folderEntry = Arrays.stream(listBody).filter(a -> a.type().equals(FileDto.FileType.FOLDER)).findFirst().orElseThrow();
        assertEquals("b", folderEntry.name());
        var now = Instant.now();
        var modified = folderEntry.modified();
        assertTrue(now.minusSeconds(5).isBefore(modified));
        assertEquals(0, folderEntry.size());

        var fileEntry = Arrays.stream(listBody).filter(a -> a.type().equals(FileDto.FileType.FILE)).findFirst().orElseThrow();
        assertEquals(randomFile.getName(), fileEntry.name());
        modified = fileEntry.modified();
        assertTrue(now.minusSeconds(5).isBefore(modified));
        assertEquals(fileSize, fileEntry.size());

        var logRequest = new LogRequestDto("tenant_a", "tenant_a_secret");
        var logBody = webTestClient.post()
                .uri("/admin/log")
                .bodyValue(logRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String[].class)
                .returnResult()
                .getResponseBody();
        assertNotNull(logBody);
        var findMe = Arrays.stream(logBody).filter(a -> a.contains("findMe")).findFirst().orElseThrow();
        assertNotNull(findMe);
    }

    @Test
    public void Deny_read_only_put() throws IOException {
        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "r", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();

        var fileSize = 10 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);

        var bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", resource);

        webTestClient.post()
                .uri("/put?path=//test/a//&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void Get_file() throws IOException {
        var registerRequest = new AccessTokenRequestDto("tenant_b", "tenant_b_secret", "realm", "subject", "rw", tomorrow);
        var webTestClient = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMinutes(1L))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(150 * 1024 * 1024))
                .build();
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();

        var fileSize = 100 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);

        var bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", resource);

        var response = webTestClient.post()
                .uri("/put?path=//test/a//")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PutFileResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(fileSize, response.bytes());

        var downloadedBytes = webTestClient.get()
                .uri("/get?file=/test/a/" + randomFile.getName())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(downloadedBytes);
        var expectedBytes = Files.readAllBytes(randomFile.toPath());
        assertArrayEquals(expectedBytes, downloadedBytes);
    }

    @Test
    public void Get_range_file() throws IOException {
        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "rw", tomorrow);
        var webTestClient = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMinutes(1L))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(150 * 1024 * 1024))
                .build();
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();

        var fileSize = 100 * 1024 * 1024;
        var firstSliceSize = 33 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);

        var bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", resource);

        var response = webTestClient.post()
                .uri("/put?path=//test/a//&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PutFileResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(fileSize, response.bytes());

        var firstSliceBytes = webTestClient.get()
                .uri("/get?file=/test/a/" + randomFile.getName())
                .header("Authorization", "Bearer " + token)
                .header("Range", String.format("bytes=0-%d", firstSliceSize - 1))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.PARTIAL_CONTENT.value())
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        var secondSliceBytes = webTestClient.get()
                .uri("/get?file=/test/a/" + randomFile.getName())
                .header("Authorization", "Bearer " + token)
                .header("Range", String.format("bytes=%d-%d", firstSliceSize, fileSize))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.PARTIAL_CONTENT.value())
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(firstSliceBytes);
        assertNotNull(secondSliceBytes);

        var combinedFile = File.createTempFile("combined", "tmp");
        Files.write(combinedFile.toPath(), firstSliceBytes);
        Files.write(combinedFile.toPath(), secondSliceBytes, StandardOpenOption.APPEND);

        var mismatch = Files.mismatch(randomFile.toPath(), combinedFile.toPath());
        assertEquals(-1, mismatch);
    }

    @Test
    public void Deny_write_only_get() {
        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "w", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();

        webTestClient.get()
                .uri("/get?file=/test/a/denied")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void Allow_list_for_blank_tenant() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("tenant_b").resolve("realm").toFile());
        var registerRequest = new AccessTokenRequestDto("tenant_b", "tenant_b_secret", "realm", "subject", "r", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();

        var response = webTestClient.get()
                .uri("/list?path=/")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileDto[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(0, response.length);
    }

    @Test
    public void Deny_write_only_list() {
        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "w", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();

        webTestClient.get()
                .uri("/list?path=/")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void Deny_write_only_delete() {
        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "wr", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();

        webTestClient.get()
                .uri("/delete?path=/no.txt")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void Delete() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("tenant_a").resolve("realm").toFile());
        var registerRequest = new AccessTokenRequestDto("tenant_a", "tenant_a_secret", "realm", "subject", "rdw", tomorrow);
        var registerResponse = webTestClient.post()
                .uri("/admin/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessTokenResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerResponse);
        var token = registerResponse.token();

        var fileSize1 = 10 * 1024 * 1024;
        var randomFile1 = randomFile(fileSize1);
        var resource1 = new FileSystemResource(randomFile1);

        var bodyBuilder1 = new MultipartBodyBuilder();
        bodyBuilder1.part("file", resource1);

        webTestClient.post()
                .uri("/put?path=//test/deleteMe//&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder1.build()))
                .exchange()
                .expectStatus().isOk();

        var fileSize2 = 10 * 1024 * 1024;
        var randomFile2 = randomFile(fileSize2);
        var resource2 = new FileSystemResource(randomFile2);

        var bodyBuilder2 = new MultipartBodyBuilder();
        bodyBuilder2.part("file", resource2);

        webTestClient.post()
                .uri("/put?path=//test/deleteMe//&hash=true")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder2.build()))
                .exchange()
                .expectStatus().isOk();

        var files = webTestClient.get()
                .uri("/list?path=///test//deleteMe")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileDto[].class)
                .returnResult()
                .getResponseBody();
        assertEquals(2, Arrays.stream(Objects.requireNonNull(files)).count());

        webTestClient.get()
                .uri("/delete?path=//test//deleteMe///" + randomFile1.getName())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        files = webTestClient.get()
                .uri("/list?path=///test//deleteMe")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileDto[].class)
                .returnResult()
                .getResponseBody();
        assertEquals(1, Arrays.stream(Objects.requireNonNull(files)).count());

        webTestClient.get()
                .uri("/delete?path=")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        files = webTestClient.get()
                .uri("/list?path=///")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileDto[].class)
                .returnResult()
                .getResponseBody();
        assertEquals(0, Arrays.stream(Objects.requireNonNull(files)).count());

        webTestClient.get()
                .uri("/delete?path=")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/delete?path=unknown")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/delete?path=../../inéVäli$")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private File randomFile(int sizeInBytes) throws IOException {
        var randomFile = File.createTempFile("depot-", ".rand");
        randomFile.deleteOnExit();

        var random = new Random();
        var fileOutputStream = new FileOutputStream(randomFile);
        var bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

        var bytes = new byte[sizeInBytes];

        random.nextBytes(bytes);
        bufferedOutputStream.write(bytes);

        bufferedOutputStream.flush();
        bufferedOutputStream.close();
        fileOutputStream.flush();
        fileOutputStream.close();

        return randomFile;
    }
}
