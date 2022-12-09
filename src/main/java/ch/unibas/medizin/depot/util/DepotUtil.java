package ch.unibas.medizin.depot.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DepotUtil {

    public static Path normalizePath(String path) {
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

    public static boolean validAbsolutPath(String candidate) {
        var candidateAsPath = Paths.get(candidate);
        var candidatePath = candidateAsPath.getParent().toString();

        if (!validPath(candidatePath)) {
            return false;
        }

        var candidateFilename = candidateAsPath.getFileName().toString();

        return validFilename(candidateFilename);
    }

    public static boolean validRealm(String candidate) {
        return isValid(candidate, false);
    }

    public static boolean validFilename(String candidate) {
        return isValid(candidate, false);
    }

    public static boolean validPath(String candidate) {
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
