package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "depot.backup=true")
public class BackupTests {

    @Autowired
    private DepotService depotService;

    @Autowired
    private DepotProperties depotProperties;

    private Path realmPath;

    @BeforeEach
    void setUp() throws IOException {
        realmPath = depotProperties.getBaseDirectory().resolve("tenant").resolve("realm").resolve("backup");
        if (Files.exists(realmPath)) {
            FileSystemUtils.deleteRecursively(realmPath);
        }
    }

    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void Backup_created_on_overwrite_with_different_content() {
        var file1 = new MockMultipartFile("file", "test.txt", "text/plain", "content v1".getBytes());
        depotService.put(file1, "backup", false);

        var file2 = new MockMultipartFile("file", "test.txt", "text/plain", "content v2".getBytes());
        depotService.put(file2, "backup", false);

        var backupDir = realmPath.resolve(".test.txt");
        assertTrue(Files.isDirectory(backupDir));
        assertTrue(Files.exists(backupDir.resolve("test.txt_1")));
    }

    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void No_backup_on_overwrite_with_same_content() {
        var file1 = new MockMultipartFile("file", "same.txt", "text/plain", "identical".getBytes());
        depotService.put(file1, "backup", false);

        var file2 = new MockMultipartFile("file", "same.txt", "text/plain", "identical".getBytes());
        depotService.put(file2, "backup", false);

        var backupDir = realmPath.resolve(".same.txt");
        assertFalse(Files.exists(backupDir));
    }

    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void Multiple_backups_numbered_sequentially() {
        var file1 = new MockMultipartFile("file", "multi.txt", "text/plain", "v1".getBytes());
        depotService.put(file1, "backup", false);

        var file2 = new MockMultipartFile("file", "multi.txt", "text/plain", "v2".getBytes());
        depotService.put(file2, "backup", false);

        var file3 = new MockMultipartFile("file", "multi.txt", "text/plain", "v3".getBytes());
        depotService.put(file3, "backup", false);

        var backupDir = realmPath.resolve(".multi.txt");
        assertTrue(Files.exists(backupDir.resolve("multi.txt_1")));
        assertTrue(Files.exists(backupDir.resolve("multi.txt_2")));
        assertFalse(Files.exists(backupDir.resolve("multi.txt_3")));
    }

    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void Backup_preserves_original_content() throws IOException {
        var originalContent = "original content";
        var file1 = new MockMultipartFile("file", "preserve.txt", "text/plain", originalContent.getBytes());
        depotService.put(file1, "backup", false);

        var file2 = new MockMultipartFile("file", "preserve.txt", "text/plain", "new content".getBytes());
        depotService.put(file2, "backup", false);

        var backupFile = realmPath.resolve(".preserve.txt").resolve("preserve.txt_1");
        assertEquals(originalContent, Files.readString(backupFile));
        assertEquals("new content", Files.readString(realmPath.resolve("preserve.txt")));
    }

    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void No_backup_on_first_upload() {
        var file = new MockMultipartFile("file", "fresh.txt", "text/plain", "content".getBytes());
        depotService.put(file, "backup", false);

        var backupDir = realmPath.resolve(".fresh.txt");
        assertFalse(Files.exists(backupDir));
    }

    @Test
    @WithMockUser(username = "tenant" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject")
    public void Backup_folders_not_in_list_response() {
        var file1 = new MockMultipartFile("file", "listed.txt", "text/plain", "v1".getBytes());
        depotService.put(file1, "backup", false);

        var file2 = new MockMultipartFile("file", "listed.txt", "text/plain", "v2".getBytes());
        depotService.put(file2, "backup", false);

        var entries = depotService.list("backup", false);
        assertEquals(1, entries.size());
        assertEquals("listed.txt", entries.getFirst().name());
    }
}
