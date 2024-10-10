package ch.unibas.medizin.depot.service;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.concurrent.Executors;

@SpringBootTest
public class DepotServiceTests {

    @MockitoBean
    private LogService logService;

    @Autowired
    private AuthorizationService authorizationService;

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
        var tokenData = authorizationService.getTokenData();

        try (var executor = Executors.newFixedThreadPool(10)) {
            for (int i = 48; i < 58; i++) {
                var bytes = new byte[sizeInBytes];
                Arrays.fill(bytes, (byte) i);
                var mockFile = new MockMultipartFile("file", "mock.txt", "application/bytes", bytes);
                executor.execute(() -> depotService.put(mockFile, "/concurrent/test/", true, tokenData));
            }
        }
    }

}
