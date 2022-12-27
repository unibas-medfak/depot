package ch.unibas.medizin.depot.service;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Random;
import java.util.concurrent.Executors;

@SpringBootTest
public class DepotServiceTests {

    private static final Logger log = LoggerFactory.getLogger(DepotServiceTests.class);

    @Autowired
    private DepotService depotService;

    @PostConstruct
    void setGlobalSecurityContext() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Test
    @WithMockUser(username = "realm" + Character.LINE_SEPARATOR + "subject")
    void Concurrent_file_write() {
        var random = new Random();
        var sizeInBytes = 10 * 1024 * 1024;
        var bytes = new byte[sizeInBytes];
        random.nextBytes(bytes);

        var mockFile = new MockMultipartFile("file", "mock.txt", "application/bytes", bytes);

        try (var executor = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < 10; i++) {
                executor.execute(() -> depotService.put(mockFile, "/", true));
            }
        }
    }

}
