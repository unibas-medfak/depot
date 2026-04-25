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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SoftDeleteTests {

    private static final String SOFT_DELETE_USER = "tenant_b" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject";
    private static final String HARD_DELETE_USER = "tenant_a" + Character.LINE_SEPARATOR + "realm" + Character.LINE_SEPARATOR + "subject";

    @Autowired
    private DepotService depotService;

    @Autowired
    private DepotProperties depotProperties;

    private Path softRealmPath;
    private Path hardRealmPath;

    @BeforeEach
    void setUp() throws IOException {
        softRealmPath = depotProperties.getBaseDirectory().resolve("tenant_b").resolve("realm").resolve("softdelete");
        hardRealmPath = depotProperties.getBaseDirectory().resolve("tenant_a").resolve("realm").resolve("softdelete");
        if (Files.exists(softRealmPath)) {
            FileSystemUtils.deleteRecursively(softRealmPath);
        }
        if (Files.exists(hardRealmPath)) {
            FileSystemUtils.deleteRecursively(hardRealmPath);
        }
    }

    @Test
    @WithMockUser(username = SOFT_DELETE_USER)
    public void Soft_delete_renames_file_with_dot_prefix() {
        var file = new MockMultipartFile("file", "doc.txt", "text/plain", "content".getBytes(UTF_8));
        depotService.put(file, "softdelete", false);

        depotService.delete("softdelete/doc.txt");

        assertFalse(Files.exists(softRealmPath.resolve("doc.txt")));
        assertTrue(Files.exists(softRealmPath.resolve(".doc.txt")));
    }

    @Test
    @WithMockUser(username = SOFT_DELETE_USER)
    public void Soft_deleted_file_preserves_content() throws IOException {
        var content = "important data";
        var file = new MockMultipartFile("file", "preserve.txt", "text/plain", content.getBytes(UTF_8));
        depotService.put(file, "softdelete", false);

        depotService.delete("softdelete/preserve.txt");

        assertEquals(content, Files.readString(softRealmPath.resolve(".preserve.txt")));
    }

    @Test
    @WithMockUser(username = SOFT_DELETE_USER)
    public void Soft_deleted_file_hidden_from_list() {
        var file = new MockMultipartFile("file", "hidden.txt", "text/plain", "x".getBytes(UTF_8));
        depotService.put(file, "softdelete", false);

        depotService.delete("softdelete/hidden.txt");

        var entries = depotService.list("softdelete", false);
        assertTrue(entries.isEmpty());
    }

    @Test
    @WithMockUser(username = SOFT_DELETE_USER)
    public void Soft_delete_collision_appends_suffix() {
        var v1 = new MockMultipartFile("file", "dup.txt", "text/plain", "v1".getBytes(UTF_8));
        depotService.put(v1, "softdelete", false);
        depotService.delete("softdelete/dup.txt");

        var v2 = new MockMultipartFile("file", "dup.txt", "text/plain", "v2".getBytes(UTF_8));
        depotService.put(v2, "softdelete", false);
        depotService.delete("softdelete/dup.txt");

        assertTrue(Files.exists(softRealmPath.resolve(".dup.txt")));
        assertTrue(Files.exists(softRealmPath.resolve(".dup.txt_1")));
    }

    @Test
    @WithMockUser(username = SOFT_DELETE_USER)
    public void Soft_delete_works_for_folders() {
        var file = new MockMultipartFile("file", "child.txt", "text/plain", "x".getBytes(UTF_8));
        depotService.put(file, "softdelete/folder", false);

        depotService.delete("softdelete/folder");

        assertFalse(Files.exists(softRealmPath.resolve("folder")));
        assertTrue(Files.isDirectory(softRealmPath.resolve(".folder")));
        assertTrue(Files.exists(softRealmPath.resolve(".folder").resolve("child.txt")));
    }

    @Test
    @WithMockUser(username = SOFT_DELETE_USER)
    public void Soft_delete_of_missing_file_is_noop() {
        assertDoesNotThrow(() -> depotService.delete("softdelete/missing.txt"));
        assertFalse(Files.exists(softRealmPath.resolve(".missing.txt")));
    }

    @Test
    @WithMockUser(username = HARD_DELETE_USER)
    public void Hard_delete_tenant_removes_file_from_disk() {
        var file = new MockMultipartFile("file", "gone.txt", "text/plain", "x".getBytes(UTF_8));
        depotService.put(file, "softdelete", false);

        depotService.delete("softdelete/gone.txt");

        assertFalse(Files.exists(hardRealmPath.resolve("gone.txt")));
        assertFalse(Files.exists(hardRealmPath.resolve(".gone.txt")));
    }
}
