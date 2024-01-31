package ch.unibas.medizin.depot.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface DepotUtil {

    String LOGFILE_NAME = "depot-access.log";

    static Path normalizePath(final String path) {
        var normalizedPath = path.replaceAll(" ", "");

        var changed = true;

        while (changed) {
            var after = normalizedPath.replaceAll("/[.]", "/").replaceAll("[.]/", "/");
            if (normalizedPath.equals(after)) {
                changed = false;
            }
            normalizedPath = after;
        }

        return Paths.get(
                normalizedPath
                .replaceAll("/+", "/")
                .replaceAll("^/", "")
                .replaceAll("/$", "")
        );
    }

    static boolean isValidAbsolutPath(final String candidate) {
        final var candidateAsPath = Paths.get(candidate);
        final var candidatePath = candidateAsPath.getParent();

        if (candidatePath != null && !isValidPath(candidatePath.toString())) {
            return false;
        }

        final var candidateFilename = candidateAsPath.getFileName().toString();

        return isValidFilename(candidateFilename);
    }

    static boolean isValidRealm(final String candidate) {
        return isValid(candidate, false);
    }

    static boolean isValidFilename(final String candidate) {
        return isValid(candidate, false);
    }

    static boolean isValidPath(final String candidate) {
        return isValid(candidate, true);
    }

    private static boolean isValid(final String candidate, final boolean allowSlash) {
        final int length = candidate.length();

        for (int i = 0; i < length; i++) {
            final var candidateChar = candidate.charAt(i);
            if (!Character.isLetterOrDigit(candidateChar)
                    && candidateChar != '.'
                    && candidateChar != '_'
                    && candidateChar != '-'
                    && candidateChar != '@'
                    && candidateChar != '%'
                    && candidateChar != '+'
                    && (!allowSlash || candidateChar != '/')) {
                        return false;
            }
        }

        return true;
    }

}
