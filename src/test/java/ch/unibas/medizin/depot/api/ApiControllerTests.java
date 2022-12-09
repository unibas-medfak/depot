package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.AccessTokenResponseDto;
import ch.unibas.medizin.depot.dto.FileDto;
import ch.unibas.medizin.depot.dto.AccessTokenRequestDto;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiControllerTests {

    private final String baseUrl = "http://localhost:";

    private final LocalDate today = LocalDate.now();

    private final LocalDate tomorrow = LocalDate.now().plus(1, ChronoUnit.DAYS);

    @LocalServerPort
    private int port;

    @Autowired
    private DepotProperties depotProperties;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void Register_client_with_valid_request() {
        var validRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "re_al-m1", "subject1", "r", tomorrow));
        var validRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", validRegisterRequest, AccessTokenResponseDto.class);
        assertEquals(HttpStatus.OK, validRegisterResponse.getStatusCode());
        assertNotNull(Objects.requireNonNull(validRegisterResponse.getBody()).token());
    }

    @Test
    @SneakyThrows
    public void Request_token_qr() {
        var validRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "re_al-m1", "subject1", "r", LocalDate.of(2050, 12, 31)));
        var validRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/qr", validRegisterRequest, byte[].class);
        assertEquals(HttpStatus.OK, validRegisterResponse.getStatusCode());
        assertNotNull(validRegisterResponse.getBody());

        var referenceBytes = IOUtils.resourceToByteArray("/qr.png");
        var actualBytes = validRegisterResponse.getBody();

        log.error("referenceBytes 1={} 2={} {}={} {}={}",
                referenceBytes[0],
                referenceBytes[1],
                referenceBytes.length - 1, referenceBytes[referenceBytes.length - 2],
                referenceBytes.length, referenceBytes[referenceBytes.length - 1]);

        log.error("actualBytes 1={} 2={} {}={} {}={}",
                actualBytes[0],
                actualBytes[1],
                actualBytes.length - 1, actualBytes[actualBytes.length - 2],
                actualBytes.length, actualBytes[actualBytes.length - 1]);

        assertArrayEquals(referenceBytes, actualBytes);
    }

    @Test
    public void Deny_client_with_invalid_realm_name() {
        var invalidRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm$_", "subject", "r", tomorrow));
        var invalidRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", invalidRegisterRequest, AccessTokenResponseDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, invalidRegisterResponse.getStatusCode());
    }

    @Test
    public void Deny_client_with_invalid_blank_in_realm_name() {
        var invalidRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "re alm", "subject", "r", tomorrow));
        var invalidRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", invalidRegisterRequest, AccessTokenResponseDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, invalidRegisterResponse.getStatusCode());
    }

    @Test
    public void Deny_client_with_invalid_subject() {
        var invalidRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", " ", "r", tomorrow));
        var invalidRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", invalidRegisterRequest, AccessTokenResponseDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, invalidRegisterResponse.getStatusCode());
    }

    @Test
    public void Deny_client_with_invalid_admin_password() {
        var invalidRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("wrong_password", "realm", "subject", "r", tomorrow));
        var invalidRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", invalidRegisterRequest, AccessTokenResponseDto.class);
        assertEquals(HttpStatus.UNAUTHORIZED, invalidRegisterResponse.getStatusCode());
    }

    @Test
    public void Deny_client_with_expiration_date_not_in_the_future() {
        var todayRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "r", today));
        var todayRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", todayRegisterRequest, AccessTokenResponseDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, todayRegisterResponse.getStatusCode());
    }

    @Test
    @Disabled
    public void Deny_requests_from_client_with_expiration_date_of_today() {
        var todayRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "r", today));
        var todayRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", todayRegisterRequest, AccessTokenResponseDto.class);
        assertEquals(HttpStatus.OK, todayRegisterResponse.getStatusCode());

        var headers = getHeaders(Objects.requireNonNull(todayRegisterResponse.getBody()).token());
        headers.setContentType(MediaType.APPLICATION_JSON);
        var listRequest = new HttpEntity<>(headers);
        var files = restTemplate.exchange(baseUrl + port + "/list?path=///test//a", HttpMethod.GET, listRequest, FileDto[].class);

        assertEquals(HttpStatus.FORBIDDEN, files.getStatusCode());
    }

    @Test
    @SneakyThrows
    public void Put_file() {
        FileUtils.deleteDirectory(depotProperties.baseDirectory().toFile());

        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "rw", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        var headers = getHeaders(Objects.requireNonNull(registerResponse.getBody()).token());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        var fileSize = 10 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);
        body.add("file", resource);

        var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
        var serverUrl = baseUrl + port + "/put?path=//test/a//&hash=true";
        var response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(fileSize, Objects.requireNonNull(response.getBody()).bytes());

        serverUrl = baseUrl + port + "/put?path=//test/a/b/&hash=false";
        response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(fileSize, Objects.requireNonNull(response.getBody()).bytes());

        headers.setContentType(MediaType.APPLICATION_JSON);
        var listRequest = new HttpEntity<>(headers);
        var files = restTemplate.exchange(baseUrl + port + "/list?path=///test//a", HttpMethod.GET, listRequest, FileDto[].class);

        var listBody = files.getBody();

        assert listBody != null;
        assertEquals(2, listBody.length);

        var folderEntry = Arrays.stream(listBody).filter(a -> a.type().equals(FileDto.FileType.FOLDER)).findFirst().orElseThrow();
        assertEquals("b", folderEntry.name());

        var fileEntry = Arrays.stream(listBody).filter(a -> a.type().equals(FileDto.FileType.FILE)).findFirst().orElseThrow();
        assertEquals(randomFile.getName(), fileEntry.name());
    }

    @Test
    public void Deny_read_only_put() {
        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "r", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        var headers = getHeaders(Objects.requireNonNull(registerResponse.getBody()).token());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        var fileSize = 10 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);
        body.add("file", resource);

        var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
        var serverUrl = baseUrl + port + "/put?path=//test/a//&hash=true";
        var response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @SneakyThrows
    public void Get_file() {
        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "rw", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        var headers = getHeaders(Objects.requireNonNull(registerResponse.getBody()).token());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        var fileSize = 100 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);
        body.add("file", resource);

        var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
        var serverUrl = baseUrl + port + "/put?path=//test/a//";
        var response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(fileSize, Objects.requireNonNull(response.getBody()).bytes());

        var getUrl = baseUrl + port + "/get?file=/test/a/" + randomFile.getName();

        var file = restTemplate.execute(getUrl, HttpMethod.GET, clientHttpRequest -> clientHttpRequest
                .getHeaders().set("Authorization", headers.getFirst("Authorization")), clientHttpResponse -> {
            var ret = File.createTempFile("download", "tmp");
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
            return ret;
        });

        var mismatch = Files.mismatch(randomFile.toPath(), file.toPath());
        assertEquals(-1, mismatch);
    }

    @Test
    @SneakyThrows
    public void Get_range_file() {
        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "rw", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        var headers = getHeaders(Objects.requireNonNull(registerResponse.getBody()).token());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        var fileSize = 100 * 1024 * 1024;
        var firstSliceSize = 33 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);
        body.add("file", resource);

        var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
        var serverUrl = baseUrl + port + "/put?path=//test/a//&hash=true";
        var response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(fileSize, Objects.requireNonNull(response.getBody()).bytes());

        var getUrl = baseUrl + port + "/get?file=/test/a/" + randomFile.getName();

        var firstSlice = restTemplate.execute(getUrl, HttpMethod.GET, clientHttpRequest -> {
            clientHttpRequest.getHeaders().set("Authorization", headers.getFirst("Authorization"));
            clientHttpRequest.getHeaders().set("Range", String.format("bytes=0-%d", firstSliceSize - 1));
        }, clientHttpResponse -> {
            var ret = File.createTempFile("download", "tmp");
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
            return ret;
        });

        var secondSlice = restTemplate.execute(getUrl, HttpMethod.GET, clientHttpRequest -> {
            clientHttpRequest.getHeaders().set("Authorization", headers.getFirst("Authorization"));
            clientHttpRequest.getHeaders().set("Range", String.format("bytes=%d-%d", firstSliceSize, fileSize));
        }, clientHttpResponse -> {
            var ret = File.createTempFile("download", "tmp");
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
            return ret;
        });

        var secondSliceBytes = Files.readAllBytes(secondSlice.toPath());

        Files.write(firstSlice.toPath(), secondSliceBytes, StandardOpenOption.APPEND);

        var mismatch = Files.mismatch(randomFile.toPath(), firstSlice.toPath());
        assertEquals(-1, mismatch);
    }

    @Test
    public void Deny_write_only_get() {
        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "w", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        var headers = getHeaders(Objects.requireNonNull(registerResponse.getBody()).token());

        var requestEntity = new HttpEntity<Void>(headers);

        var getUrl = baseUrl + port + "/get?file=/test/a/denied";

        var response = restTemplate.exchange(getUrl, HttpMethod.GET, requestEntity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void Deny_write_only_list() {
        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "w", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        var headers = getHeaders(Objects.requireNonNull(registerResponse.getBody()).token());

        var requestEntity = new HttpEntity<Void>(headers);

        var getUrl = baseUrl + port + "/list?path=/";

        var response = restTemplate.exchange(getUrl, HttpMethod.GET, requestEntity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    private HttpHeaders getHeaders(String token) {
        var headers = new HttpHeaders();
        var bearer = "Bearer " + token;
        headers.set("Authorization", bearer);

        return headers;
    }

    @SneakyThrows
    private File randomFile(int sizeInBytes) {
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
