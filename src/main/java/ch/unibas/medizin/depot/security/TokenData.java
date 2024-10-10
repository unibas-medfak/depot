package ch.unibas.medizin.depot.security;

import java.nio.file.Path;

public record TokenData(String tenant, Path basePath, String subject) {
}
