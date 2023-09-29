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
    void validRealm() {
        assertTrue(DepotUtil.isValidRealm("1abc.-+@_%"));
        assertFalse(DepotUtil.isValidRealm("1abc.-&@_%"));
        assertFalse(DepotUtil.isValidRealm("1abc.-/@_%"));
    }

    @Test
    void validFilename() {
        assertTrue(DepotUtil.isValidFilename("1abc.-@_%.txt"));
        assertFalse(DepotUtil.isValidFilename("1abc.-!@_%.txt"));
        assertFalse(DepotUtil.isValidFilename("1abc.-/@_%.txt"));
    }

    @Test
    void validPath() {
        assertTrue(DepotUtil.isValidPath("1abc.-@_%.txt"));
        assertFalse(DepotUtil.isValidFilename("1abc.-!@_%.txt"));
        assertFalse(DepotUtil.isValidFilename("1abc.-/@_%.txt"));
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

}
