package ch.unibas.medizin.depot.service;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.concurrent.Executors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class DepotServiceTests {

    @Autowired
    private DepotService depotService;

    @PostConstruct
    public void setGlobalSecurityContext() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void Concurrent_file_write() {
        var sizeInBytes = 10 * 1024 * 1024;

        try (var executor = Executors.newFixedThreadPool(10)) {
            for (int i = 48; i < 58; i++) {
                var bytes = new byte[sizeInBytes];
                Arrays.fill(bytes, (byte) i);
                var mockFile = new MockMultipartFile("file", "mock.txt", "application/bytes", bytes);
                executor.execute(() -> depotService.put(mockFile, "/concurrent/test/", true));
            }
        }
    }

    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void Put_rejects_parent_directory_traversal_in_filename() {
        var mockFile = new MockMultipartFile("file", "../../../evil.txt", "text/plain", "x".getBytes(UTF_8));
        assertThrows(IllegalArgumentException.class,
                () -> depotService.put(mockFile, "/traversal/", false));
    }

}
