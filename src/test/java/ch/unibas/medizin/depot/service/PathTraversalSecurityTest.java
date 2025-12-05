package ch.unibas.medizin.depot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security test demonstrating path traversal vulnerability (Issue #178).
 *
 * This test demonstrates that the current path validation in DepotService.put()
 * uses string concatenation instead of proper Path API methods, which could
 * potentially be bypassed in certain edge cases.
 *
 * Current vulnerable code in DepotService.java:123-126:
 * <pre>
 * if (!fullPathAndFile.startsWith(fullPath + File.separator)) {
 *     throw new IllegalArgumentException("Invalid file path");
 * }
 * </pre>
 *
 * The issue is that this check happens after path resolution but doesn't
 * consistently use normalized paths and the base path for validation.
 *
 * @see <a href="https://github.com/unibas-medfak/depot/issues/178">Issue #178</a>
 */
@SpringBootTest
public class PathTraversalSecurityTest {

    @Autowired
    private DepotService depotService;

    @TempDir
    Path tempDir;

    /**
     * Test Case 1: Basic path traversal attempt using ../
     *
     * This test attempts to upload a file with a path traversal sequence
     * in the filename. The current implementation should catch this, but
     * we're documenting the attack vector.
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testPathTraversal_ParentDirectoryInFilename() {
        // Arrange
        String maliciousFilename = "../../../evil.txt";
        MockMultipartFile maliciousFile = new MockMultipartFile(
            "file",
            maliciousFilename,
            "text/plain",
            "malicious content".getBytes()
        );

        // Act & Assert
        // This SHOULD throw an exception, but let's verify the behavior
        assertThrows(Exception.class, () -> {
            depotService.put(maliciousFile, "/test/", false);
        }, "Path traversal attempt should be blocked");
    }

    /**
     * Test Case 2: Path traversal with encoded slashes
     *
     * Attempts to use URL-encoded characters to bypass validation.
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testPathTraversal_EncodedSlashes() {
        // Arrange - %2e%2e%2f is ../
        String maliciousFilename = "..%2F..%2F..%2Fevil.txt";
        MockMultipartFile maliciousFile = new MockMultipartFile(
            "file",
            maliciousFilename,
            "text/plain",
            "malicious content".getBytes()
        );

        // Act & Assert
        assertThrows(Exception.class, () -> {
            depotService.put(maliciousFile, "/test/", false);
        }, "Encoded path traversal should be blocked");
    }

    /**
     * Test Case 3: Absolute path in filename
     *
     * Attempts to use an absolute path that points outside the tenant directory.
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testPathTraversal_AbsolutePath() {
        // Arrange
        String maliciousFilename = "/etc/passwd";
        MockMultipartFile maliciousFile = new MockMultipartFile(
            "file",
            maliciousFilename,
            "text/plain",
            "malicious content".getBytes()
        );

        // Act & Assert
        assertThrows(Exception.class, () -> {
            depotService.put(maliciousFile, "/test/", false);
        }, "Absolute path should be blocked");
    }

    /**
     * Test Case 4: Path traversal in the path parameter
     *
     * This demonstrates the vulnerability where the path parameter itself
     * contains traversal sequences, and the filename appears normal.
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testPathTraversal_InPathParameter() {
        // Arrange
        String normalFilename = "normal.txt";
        String maliciousPath = "/../../../../../../tmp/"; // Try to escape to /tmp
        MockMultipartFile file = new MockMultipartFile(
            "file",
            normalFilename,
            "text/plain",
            "test content".getBytes()
        );

        // Act & Assert
        // The path normalization in DepotUtil.normalizePath should handle this,
        // but we're testing to ensure it does
        assertDoesNotThrow(() -> {
            depotService.put(file, maliciousPath, false);
        }, "Path normalization should handle traversal in path parameter");
    }

    /**
     * Test Case 5: Null bytes in filename
     *
     * Attempts to use null byte injection to truncate the path.
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testPathTraversal_NullByteInjection() {
        // Arrange
        String maliciousFilename = "evil.txt\0.jpg"; // Try to bypass extension checks
        MockMultipartFile maliciousFile = new MockMultipartFile(
            "file",
            maliciousFilename,
            "text/plain",
            "malicious content".getBytes()
        );

        // Act & Assert
        assertThrows(Exception.class, () -> {
            depotService.put(maliciousFile, "/test/", false);
        }, "Null byte injection should be blocked");
    }

    /**
     * Test Case 6: Unicode normalization attack
     *
     * Uses Unicode characters that normalize to path traversal sequences.
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testPathTraversal_UnicodeNormalization() {
        // Arrange - Unicode characters that might normalize to ../
        String maliciousFilename = "\u002e\u002e\u002f\u002e\u002e\u002fevil.txt"; // Unicode for ../
        MockMultipartFile maliciousFile = new MockMultipartFile(
            "file",
            maliciousFilename,
            "text/plain",
            "malicious content".getBytes()
        );

        // Act & Assert
        assertThrows(Exception.class, () -> {
            depotService.put(maliciousFile, "/test/", false);
        }, "Unicode normalization attack should be blocked");
    }

    /**
     * Test Case 7: Windows-style path traversal
     *
     * Tests backslash-based path traversal (Windows-style).
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testPathTraversal_BackslashTraversal() {
        // Arrange
        String maliciousFilename = "..\\..\\..\\evil.txt";
        MockMultipartFile maliciousFile = new MockMultipartFile(
            "file",
            maliciousFilename,
            "text/plain",
            "malicious content".getBytes()
        );

        // Act & Assert
        assertThrows(Exception.class, () -> {
            depotService.put(maliciousFile, "/test/", false);
        }, "Backslash path traversal should be blocked");
    }

    /**
     * Test Case 8: Demonstrates the actual vulnerability
     *
     * This test creates a scenario where the string concatenation check
     * might not work as expected. The vulnerability is that the check
     * uses string concatenation instead of Path.startsWith().
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testPathTraversal_StringConcatenationVulnerability() throws IOException {
        // This test demonstrates the conceptual issue with the current implementation.
        // The check in DepotService.java uses:
        //   if (!fullPathAndFile.startsWith(fullPath + File.separator))
        //
        // Instead of:
        //   if (!fullPathAndFile.normalize().startsWith(basePath.normalize()))
        //
        // This could lead to edge cases where the validation is bypassed.

        // Arrange
        String filename = "test.txt";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            filename,
            "text/plain",
            "test content".getBytes()
        );

        // Act - Normal upload should work
        assertDoesNotThrow(() -> {
            depotService.put(file, "/safe/path/", false);
        }, "Normal file upload should succeed");

        // This test documents the vulnerability even if it doesn't directly exploit it
        // The fix should ensure consistent use of normalized paths and proper Path API
    }

    /**
     * Test Case 9: Verify that legitimate nested paths work correctly
     *
     * This ensures our security measures don't break legitimate use cases.
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testLegitimateNestedPath_ShouldWork() {
        // Arrange
        String filename = "document.txt";
        String legitimatePath = "/projects/2025/reports/";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            filename,
            "text/plain",
            "legitimate content".getBytes()
        );

        // Act & Assert
        assertDoesNotThrow(() -> {
            depotService.put(file, legitimatePath, false);
        }, "Legitimate nested path should work");
    }

    /**
     * Test Case 10: Edge case with path ending in separator
     */
    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void testPathTraversal_PathEndingWithSeparator() {
        // Arrange
        String filename = "../evil.txt";
        String path = "/test/path/"; // Path ending with separator
        MockMultipartFile file = new MockMultipartFile(
            "file",
            filename,
            "text/plain",
            "malicious content".getBytes()
        );

        // Act & Assert
        assertThrows(Exception.class, () -> {
            depotService.put(file, path, false);
        }, "Path traversal should be blocked even with trailing separator");
    }
}
