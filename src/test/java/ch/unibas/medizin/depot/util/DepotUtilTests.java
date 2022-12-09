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
        assertTrue(DepotUtil.validRealm("1abc.-+@_%"));
        assertFalse(DepotUtil.validRealm("1abc.-&@_%"));
        assertFalse(DepotUtil.validRealm("1abc.-/@_%"));
    }

    @Test
    void validFilename() {
        assertTrue(DepotUtil.validFilename("1abc.-@_%.txt"));
        assertFalse(DepotUtil.validFilename("1abc.-!@_%.txt"));
        assertFalse(DepotUtil.validFilename("1abc.-/@_%.txt"));
    }

    @Test
    void validPath() {
        assertTrue(DepotUtil.validPath("1abc.-@_%.txt"));
        assertFalse(DepotUtil.validFilename("1abc.-!@_%.txt"));
        assertFalse(DepotUtil.validFilename("1abc.-/@_%.txt"));
    }

}
