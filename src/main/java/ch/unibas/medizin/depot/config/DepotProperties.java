package ch.unibas.medizin.depot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "depot")
public record DepotProperties(
        Path baseDirectory,
        String adminPassword,
        String jwtSecret,
        String timeZone
) {
}
