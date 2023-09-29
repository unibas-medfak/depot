package ch.unibas.medizin.depot.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface DepotUtil {

    String LOGFILE_NAME = "depot-access.log";

    static Path normalizePath(String path) {
        path = path.replaceAll(" ", "");

        var changed = true;

        while (changed) {
            var after = path.replaceAll("/[.]", "/").replaceAll("[.]/", "/");
            if (path.equals(after)) {
                changed = false;
            }
            path = after;
        }

        return Paths.get(
                path
                .replaceAll("/+", "/")
                .replaceAll("^/", "")
                .replaceAll("/$", "")
        );
    }

    static boolean isValidAbsolutPath(String candidate) {
        var candidateAsPath = Paths.get(candidate);
        var candidatePath = candidateAsPath.getParent();

        if (candidatePath != null && !isValidPath(candidatePath.toString())) {
            return false;
        }

        var candidateFilename = candidateAsPath.getFileName().toString();

        return isValidFilename(candidateFilename);
    }

    static boolean isValidRealm(String candidate) {
        return isValid(candidate, false);
    }

    static boolean isValidFilename(String candidate) {
        return isValid(candidate, false);
    }

    static boolean isValidPath(String candidate) {
        return isValid(candidate, true);
    }

    private static boolean isValid(String candidate, boolean allowSlash) {
        int length = candidate.length();

        for (int i = 0; i < length; i++) {
            var candidateChar = candidate.charAt(i);
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
