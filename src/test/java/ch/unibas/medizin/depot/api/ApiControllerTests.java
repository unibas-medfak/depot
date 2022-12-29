package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

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
        assertNotNull(validRegisterResponse.getBody());
        assertNotNull(validRegisterResponse.getBody().token());
    }

    @Test
    public void Request_token() {
        var validRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "re_al-m1", "subject1", "r", LocalDate.of(2037, 11, 13)));
        var validRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", validRegisterRequest, AccessTokenResponseDto.class);
        assertEquals(HttpStatus.OK, validRegisterResponse.getStatusCode());

        assertNotNull(validRegisterResponse.getBody());
        var referenceToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJtb2RlIjoiciIsInN1YiI6InN1YmplY3QxIiwiaXNzIjoiZGVwb3QiLCJyZWFsbSI6InJlX2FsLW0xIiwiZXhwIjoyMTQxNjgzMjAwfQ.16258Z4X6lb4FUgNI-djrkLxDLuSlsT8kE0Lvhf3gso";
        assertEquals(referenceToken, validRegisterResponse.getBody().token());
    }

    @Test
    public void Request_token_qr() throws IOException {
        var validRegisterRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "re_al-m2", "subject2", "w", LocalDate.of(2050, 12, 31)));
        var validRegisterResponse = restTemplate.postForEntity(baseUrl + port + "/admin/qr", validRegisterRequest, byte[].class);
        assertEquals(HttpStatus.OK, validRegisterResponse.getStatusCode());

        assertNotNull(validRegisterResponse.getBody());
        // Files.write(new File("qr.png").toPath(), validRegisterResponse.getBody());

        var referenceBytes = IOUtils.resourceToByteArray("/qr.png");
        assertArrayEquals(referenceBytes, validRegisterResponse.getBody());
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
        assertNotNull(todayRegisterResponse.getBody());
        var headers = getHeaders(todayRegisterResponse.getBody().token());
        headers.setContentType(MediaType.APPLICATION_JSON);
        var listRequest = new HttpEntity<>(headers);
        var files = restTemplate.exchange(baseUrl + port + "/list?path=///test//a", HttpMethod.GET, listRequest, FileDto[].class);

        assertEquals(HttpStatus.FORBIDDEN, files.getStatusCode());
    }

    @Test
    public void Deny_file_with_invalid_name() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("realm").toFile());

        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "rw", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        assertNotNull(registerResponse.getBody());
        var headers = getHeaders(registerResponse.getBody().token());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        var bytes = new byte[] { 1 };

        var byteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "/";
            }
        };

        body.add("file", byteArrayResource);

        var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
        var serverUrl = baseUrl + port + "/put?path=/test&hash=true";
        var response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void Deny_write_file_to_folder_with_same_name() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("realm").toFile());

        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "rw", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        assertNotNull(registerResponse.getBody());
        var headers = getHeaders(registerResponse.getBody().token());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var bytes = new byte[] { 1 };

        var folderBody = new LinkedMultiValueMap<String, Object>();
        var folderByteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "folder";
            }
        };
        folderBody.add("file", folderByteArrayResource);

        var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(folderBody, headers);
        var serverUrl = baseUrl + port + "/put?path=/&hash=true";
        var response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var fileBody = new LinkedMultiValueMap<String, Object>();
        var fileByteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "file.txt";
            }
        };
        fileBody.add("file", fileByteArrayResource);

        requestEntity = new HttpEntity<>(fileBody, headers);
        serverUrl = baseUrl + port + "/put?path=/folder&hash=true";
        response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void Deny_write_folder_to_file_with_same_name() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("realm").toFile());

        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "rw", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        assertNotNull(registerResponse.getBody());
        var headers = getHeaders(registerResponse.getBody().token());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var bytes = new byte[] { 1 };

        var fileBody = new LinkedMultiValueMap<String, Object>();
        var fileByteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "file.txt";
            }
        };
        fileBody.add("file", fileByteArrayResource);

        var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(fileBody, headers);
        var serverUrl = baseUrl + port + "/put?path=/folder&hash=true";
        var response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var folderBody = new LinkedMultiValueMap<String, Object>();
        var folderByteArrayResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "folder";
            }
        };
        folderBody.add("file", folderByteArrayResource);

        requestEntity = new HttpEntity<>(folderBody, headers);
        serverUrl = baseUrl + port + "/put?path=/&hash=true";
        response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void Put_file() throws IOException {
        FileUtils.deleteDirectory(depotProperties.getBaseDirectory().resolve("realm").toFile());

        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "rw", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        assertNotNull(registerResponse.getBody());
        var headers = getHeaders(registerResponse.getBody().token());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        var fileSize = 10 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);
        body.add("file", resource);

        var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
        var serverUrl = baseUrl + port + "/put?path=//test/findMe//&hash=true";
        var response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertNotNull(response.getBody());
        assertEquals(fileSize, response.getBody().bytes());

        serverUrl = baseUrl + port + "/put?path=//test/findMe/b/&hash=false";
        response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertNotNull(response.getBody());
        assertEquals(fileSize, response.getBody().bytes());

        headers.setContentType(MediaType.APPLICATION_JSON);
        var listRequest = new HttpEntity<>(headers);
        var listResponse = restTemplate.exchange(baseUrl + port + "/list?path=///test//findMe", HttpMethod.GET, listRequest, FileDto[].class);
        var listBody = listResponse.getBody();
        assertNotNull(listBody);
        assertEquals(2, listBody.length);

        var folderEntry = Arrays.stream(listBody).filter(a -> a.type().equals(FileDto.FileType.FOLDER)).findFirst().orElseThrow();
        assertEquals("b", folderEntry.name());

        var fileEntry = Arrays.stream(listBody).filter(a -> a.type().equals(FileDto.FileType.FILE)).findFirst().orElseThrow();
        assertEquals(randomFile.getName(), fileEntry.name());

        var logRequest = new HttpEntity<>(new LogRequestDto("admin_secret"));
        var logResponse = restTemplate.postForEntity(baseUrl + port + "/admin/log", logRequest, String[].class);
        var logBody = logResponse.getBody();
        assertNotNull(logBody);
        var findMe = Arrays.stream(logBody).filter(a -> a.contains("findMe")).findFirst().orElseThrow();
        assertNotNull(findMe);
    }

    @Test
    public void Deny_read_only_put() throws IOException {
        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "r", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        assertNotNull(registerResponse.getBody());
        var headers = getHeaders(registerResponse.getBody().token());
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
    public void Get_file() throws IOException {
        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "rw", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        assertNotNull(registerResponse.getBody());
        var headers = getHeaders(registerResponse.getBody().token());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        var fileSize = 100 * 1024 * 1024;
        var randomFile = randomFile(fileSize);
        var resource = new FileSystemResource(randomFile);
        body.add("file", resource);

        var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
        var serverUrl = baseUrl + port + "/put?path=//test/a//";
        var response = restTemplate.postForEntity(serverUrl, requestEntity, PutFileResponseDto.class);

        assertNotNull(response.getBody());
        assertEquals(fileSize, response.getBody().bytes());

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
    public void Get_range_file() throws IOException {
        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "rw", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        assertNotNull(registerResponse.getBody());
        var headers = getHeaders(registerResponse.getBody().token());
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

        assertNotNull(response.getBody());
        assertEquals(fileSize, response.getBody().bytes());

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

        assertNotNull(registerResponse.getBody());
        var headers = getHeaders(registerResponse.getBody().token());

        var requestEntity = new HttpEntity<Void>(headers);

        var getUrl = baseUrl + port + "/get?file=/test/a/denied";

        var response = restTemplate.exchange(getUrl, HttpMethod.GET, requestEntity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void Deny_write_only_list() {
        var registerRequest = new HttpEntity<>(new AccessTokenRequestDto("admin_secret", "realm", "subject", "w", tomorrow));
        var registerResponse = restTemplate.postForEntity(baseUrl + port + "/admin/register", registerRequest, AccessTokenResponseDto.class);

        assertNotNull(registerResponse.getBody());
        var headers = getHeaders(registerResponse.getBody().token());

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
