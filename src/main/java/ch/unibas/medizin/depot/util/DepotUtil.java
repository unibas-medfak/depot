package ch.unibas.medizin.depot.util;

import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@NullMarked
public interface DepotUtil {

    String LOGFILE_NAME = "depot-access.log";

    Pattern SLASH_DOT = Pattern.compile("/[.]");
    Pattern DOT_SLASH = Pattern.compile("[.]/");
    Pattern MULTIPLE_SLASHES = Pattern.compile("/+");
    Pattern LEADING_SLASH = Pattern.compile("^/");
    Pattern TRAILING_SLASH = Pattern.compile("/$");

    static Path normalizePath(final String path) {
        var normalizedPath = path.replace(" ", "");

        var changed = true;

        while (changed) {
            var after = DOT_SLASH.matcher(SLASH_DOT.matcher(normalizedPath).replaceAll("/")).replaceAll("/");
            if (normalizedPath.equals(after)) {
                changed = false;
            }
            normalizedPath = after;
        }

        normalizedPath = MULTIPLE_SLASHES.matcher(normalizedPath).replaceAll("/");
        normalizedPath = LEADING_SLASH.matcher(normalizedPath).replaceAll("");
        normalizedPath = TRAILING_SLASH.matcher(normalizedPath).replaceAll("");

        return Paths.get(normalizedPath);
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

    static boolean isValidTenantOrRealm(final String candidate) {
        return isValid(candidate, false);
    }

    static boolean isValidFilename(final String candidate) {
        return !candidate.startsWith(".") && isValid(candidate, false);
    }

    static boolean isValidPath(final String candidate) {
        if (!isValid(candidate, true)) {
            return false;
        }
        for (final var segment : candidate.split("/", -1)) {
            if (segment.startsWith(".")) {
                return false;
            }
        }
        return true;
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
                    && candidateChar != '+'
                    && (!allowSlash || candidateChar != '/')) {
                return false;
            }
        }

        return true;
    }

    static boolean isValidSubject(String candidate) {
        int length = candidate.length();

        for (int i = 0; i < length; i++) {
            var candidateChar = candidate.charAt(i);

            if (Character.isISOControl(candidateChar)) {
                return false;
            }
        }

        return true;
    }

}
