package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for symbolic link protection in DepotService.
 * These tests verify that the service properly detects and blocks symbolic link attacks
 * that could be used to bypass tenant isolation.
 */
@SpringBootTest
public class SymbolicLinkProtectionTests {

    private static final String TEST_USERNAME = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject";

    @Autowired
    private DepotService depotService;

    @Autowired
    private DepotProperties depotProperties;

    private Path testBasePath;
    private Path tenantPath;
    private Path realmPath;
    private Path targetFile;
    private Path symlinkPath;

    @PostConstruct
    public void setGlobalSecurityContext() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @BeforeEach
    public void setUp() throws IOException {
        testBasePath = depotProperties.getBaseDirectory();
        tenantPath = testBasePath.resolve("tenant");
        realmPath = tenantPath.resolve("realm");

        // Create the directory structure
        Files.createDirectories(realmPath);

        // Create a regular file for testing
        targetFile = realmPath.resolve("test.txt");
        Files.writeString(targetFile, "test content");
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up symbolic links
        if (symlinkPath != null && Files.exists(symlinkPath)) {
            Files.delete(symlinkPath);
            symlinkPath = null;
        }

        // Clean up test files
        if (targetFile != null && Files.exists(targetFile)) {
            Files.delete(targetFile);
        }

        // Clean up directories
        if (realmPath != null && Files.exists(realmPath)) {
            Files.deleteIfExists(realmPath);
        }
        if (tenantPath != null && Files.exists(tenantPath)) {
            Files.deleteIfExists(tenantPath);
        }
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testGetFile_WithoutSymlink_Success() {
        // Normal file access should work
        assertDoesNotThrow(() -> {
            var resource = depotService.get("test.txt");
            assertNotNull(resource);
            assertTrue(resource.exists());
        });
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testGetFile_WithSymlinkInPath_ThrowsSecurityException() throws IOException {
        // Skip test if symbolic links are allowed (configuration dependent)
        if (depotProperties.isAllowSymbolicLinks()) {
            return;
        }

        // Create a subdirectory and a symlink pointing to it
        Path subDir = realmPath.resolve("subdir");
        Files.createDirectories(subDir);
        Path fileInSubDir = subDir.resolve("file.txt");
        Files.writeString(fileInSubDir, "content");

        symlinkPath = realmPath.resolve("symlink");
        Files.createSymbolicLink(symlinkPath, subDir);

        // Try to access file through symlink - should fail
        assertThrows(SecurityException.class, () -> {
            depotService.get("symlink/file.txt");
        });

        // Clean up
        Files.delete(fileInSubDir);
        Files.delete(subDir);
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testListDirectory_WithSymlink_ThrowsSecurityException() throws IOException {
        // Skip test if symbolic links are allowed
        if (depotProperties.isAllowSymbolicLinks()) {
            return;
        }

        // Create a directory and a symlink to it
        Path realDir = realmPath.resolve("realdir");
        Files.createDirectories(realDir);

        symlinkPath = realmPath.resolve("linkdir");
        Files.createSymbolicLink(symlinkPath, realDir);

        // Try to list the symlinked directory - should fail
        assertThrows(SecurityException.class, () -> {
            depotService.list("linkdir");
        });

        // Clean up
        Files.delete(realDir);
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testPutFile_ToSymlinkedDirectory_ThrowsSecurityException() throws IOException {
        // Skip test if symbolic links are allowed
        if (depotProperties.isAllowSymbolicLinks()) {
            return;
        }

        // Create a real directory and a symlink to it
        Path realDir = realmPath.resolve("realdir");
        Files.createDirectories(realDir);

        symlinkPath = realmPath.resolve("linkdir");
        Files.createSymbolicLink(symlinkPath, realDir);

        // Try to upload a file to the symlinked directory - should fail
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "upload.txt",
                "text/plain",
                "test content".getBytes()
        );

        assertThrows(SecurityException.class, () -> {
            depotService.put(mockFile, "linkdir", false);
        });

        // Clean up
        Files.delete(realDir);
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testDeleteFile_ThroughSymlink_ThrowsSecurityException() throws IOException {
        // Skip test if symbolic links are allowed
        if (depotProperties.isAllowSymbolicLinks()) {
            return;
        }

        // Create a directory with a file
        Path realDir = realmPath.resolve("realdir");
        Files.createDirectories(realDir);
        Path fileToDelete = realDir.resolve("file.txt");
        Files.writeString(fileToDelete, "content");

        // Create a symlink to the directory
        symlinkPath = realmPath.resolve("linkdir");
        Files.createSymbolicLink(symlinkPath, realDir);

        // Try to delete through the symlink - should fail
        assertThrows(SecurityException.class, () -> {
            depotService.delete("linkdir/file.txt");
        });

        // Clean up
        Files.delete(fileToDelete);
        Files.delete(realDir);
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testListDirectory_WithoutSymlink_Success() {
        // Normal directory listing should work
        assertDoesNotThrow(() -> {
            var files = depotService.list("");
            assertNotNull(files);
        });
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testPutFile_NormalPath_Success() {
        // Normal file upload should work
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "newfile.txt",
                "text/plain",
                "test content".getBytes()
        );

        assertDoesNotThrow(() -> {
            var response = depotService.put(mockFile, "", false);
            assertNotNull(response);
        });

        // Clean up
        try {
            Path uploadedFile = realmPath.resolve("newfile.txt");
            if (Files.exists(uploadedFile)) {
                Files.delete(uploadedFile);
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testSymlinkAtRealmLevel_ThrowsSecurityException() throws IOException {
        // Skip test if symbolic links are allowed
        if (depotProperties.isAllowSymbolicLinks()) {
            return;
        }

        // Create a second realm directory
        Path otherRealm = tenantPath.resolve("otherrealm");
        Files.createDirectories(otherRealm);
        Path sensitiveFile = otherRealm.resolve("sensitive.txt");
        Files.writeString(sensitiveFile, "sensitive data");

        // Replace realm with a symlink to otherrealm
        // This simulates an attack where filesystem access is used to create a symlink
        // Note: This test is conceptual - in reality, we can't replace the realm directory
        // while the test is using it. Instead, we test with a symlink inside the realm.

        // Create a symlink inside realm pointing to otherrealm
        symlinkPath = realmPath.resolve("attack");
        Files.createSymbolicLink(symlinkPath, otherRealm);

        // Try to access through the symlink - should fail
        assertThrows(SecurityException.class, () -> {
            depotService.get("attack/sensitive.txt");
        });

        // Clean up
        Files.delete(sensitiveFile);
        Files.delete(otherRealm);
    }
}
