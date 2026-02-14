package ch.unibas.medizin.depot.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class DepotUtilTests {

    @Test
    void normalizePath() {
        assertEquals(Paths.get("foo/bar/baz/foo.txt"), DepotUtil.normalizePath(" foo/../ /bar//baz /foo.txt/"));
        assertEquals(Paths.get("foo/bar/baz"), DepotUtil.normalizePath("/ foo/ //.././../.  . /ba r/baz/ "));
        assertEquals(Paths.get("foo/bar.foo+buz@baz.foo/bar/baz"), DepotUtil.normalizePath("/ foo/bar.foo+buz@baz.foo/./../.  . /ba r/baz/ "));
        assertEquals(Paths.get("foo/bar/baz.bar"), DepotUtil.normalizePath(" /foo./ba r/baz.bar/../ "));
        assertEquals(Paths.get("foo/bar.foo@foo.bar.baz/baz/Fo0o.jpeg"), DepotUtil.normalizePath("/ foo/ /bar.foo@foo.bar.baz/.././.. ../. . . /baz/ Fo0o.jpeg"));
    }

    @Test
    void validTenantOrRealm() {
        assertTrue(DepotUtil.isValidTenantOrRealm("1abc.-+@_%"));
        assertFalse(DepotUtil.isValidTenantOrRealm("1abc.-&@_%"));
        assertFalse(DepotUtil.isValidTenantOrRealm("1abc.-/@_%"));
    }

    @Test
    void validFilename() {
        assertTrue(DepotUtil.isValidFilename("1abc.-@_%.txt"));
        assertFalse(DepotUtil.isValidFilename("1abc.-!@_%.txt"));
        assertFalse(DepotUtil.isValidFilename("1abc.-/@_%.txt"));
    }

    @Test
    void dotFilenameRejected() {
        assertFalse(DepotUtil.isValidFilename(".hidden"));
        assertFalse(DepotUtil.isValidFilename(".hidden.txt"));
        assertFalse(DepotUtil.isValidFilename(".report.pdf"));
        assertTrue(DepotUtil.isValidFilename("report.pdf"));
    }

    @Test
    void validPath() {
        assertTrue(DepotUtil.isValidPath("1abc.-@_%.txt"));
        assertFalse(DepotUtil.isValidFilename("1abc.-!@_%.txt"));
        assertFalse(DepotUtil.isValidFilename("1abc.-/@_%.txt"));
    }

    @Test
    void dotPathSegmentRejected() {
        assertFalse(DepotUtil.isValidPath(".hidden"));
        assertFalse(DepotUtil.isValidPath(".hidden/file.txt"));
        assertFalse(DepotUtil.isValidPath("folder/.hidden"));
        assertFalse(DepotUtil.isValidPath("folder/.hidden/sub"));
        assertTrue(DepotUtil.isValidPath("folder/file.txt"));
        assertTrue(DepotUtil.isValidPath("a.b/c.d"));
    }

    @Test
    void validAbsolutePath() {
        assertTrue(DepotUtil.isValidAbsolutPath("cat.jpeg"));
        assertTrue(DepotUtil.isValidAbsolutPath("/cat.jpeg"));
        assertTrue(DepotUtil.isValidAbsolutPath("animals/cat.jpeg"));
        assertTrue(DepotUtil.isValidAbsolutPath("/animals/cat.jpeg"));

        assertFalse(DepotUtil.isValidAbsolutPath("/abcd/a#b.txt"));
        assertFalse(DepotUtil.isValidAbsolutPath("abc#d/a.txt"));
        assertFalse(DepotUtil.isValidAbsolutPath("//abc%d/a$c.txt"));
    }

    @Test
    void dotAbsolutePathRejected() {
        assertFalse(DepotUtil.isValidAbsolutPath(".hidden"));
        assertFalse(DepotUtil.isValidAbsolutPath("/.hidden"));
        assertFalse(DepotUtil.isValidAbsolutPath("folder/.hidden"));
        assertFalse(DepotUtil.isValidAbsolutPath(".hidden/file.txt"));
        assertFalse(DepotUtil.isValidAbsolutPath("folder/.hidden/file.txt"));
        assertTrue(DepotUtil.isValidAbsolutPath("folder/file.txt"));
        assertTrue(DepotUtil.isValidAbsolutPath("a.b/c.d"));
    }

}
