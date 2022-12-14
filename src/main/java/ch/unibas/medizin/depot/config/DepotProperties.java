package ch.unibas.medizin.depot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@ConfigurationProperties(prefix = "depot")
public class DepotProperties {

    private static final Logger log = LoggerFactory.getLogger(DepotProperties.class);

    private final String host;

    private final Path baseDirectory;

    private final String timeZone;

    private final String adminPassword;

    private final String jwtSecret;

    public DepotProperties(String host, Path baseDirectory, String timeZone, String adminPassword, String jwtSecret) {
        this.host = host;
        this.baseDirectory = baseDirectory;
        this.timeZone = timeZone;
        this.adminPassword = getAdminPassword(baseDirectory, adminPassword);
        this.jwtSecret = getJwtSecret(baseDirectory, jwtSecret);
    }

    public String getHost() {
        return host;
    }

    public Path getBaseDirectory() {
        return baseDirectory;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    private String getAdminPassword(Path baseDirectory, String adminPasswordFromProperties) {
        if (StringUtils.hasText(adminPasswordFromProperties)) {
            return adminPasswordFromProperties;
        }

        log.info("No admin password found in application.properties");

        var adminPasswordFilename = "adminpw.txt";

        try {
            var encodedAdminPassword = Files.readString(baseDirectory.resolve(adminPasswordFilename));
            log.info("Admin password read from {}", adminPasswordFilename);
            return encodedAdminPassword;
        } catch (IOException e) {
            log.info("No admin password found in {}", adminPasswordFilename);

            var randomPassword = UUID.randomUUID().toString();
            log.error("!!! ADMIN PASSWORD = {} !!!", randomPassword);

            var passwordEncoder = new BCryptPasswordEncoder();
            var encodedAdminPassword = passwordEncoder.encode(randomPassword);

            try {
                Files.createDirectories(baseDirectory);
                Files.writeString(baseDirectory.resolve(adminPasswordFilename), encodedAdminPassword);
                return encodedAdminPassword;
            } catch (IOException ex) {
                log.error("Error while writing {}", adminPasswordFilename, ex);
                throw new FatalBeanException("Failed to configure DepotProperties!");
            }
        }
    }

    private String getJwtSecret(Path baseDirectory, String jwtSecretFromProperties) {
        if (StringUtils.hasText(jwtSecretFromProperties)) {
            return jwtSecretFromProperties;
        }

        log.info("No JWT secret found in application.properties");

        var jwtSecretFileName = "jwtsecret.txt";

        try {
            var jwtSecretFromFile = Files.readString(baseDirectory.resolve(jwtSecretFileName));
            log.info("JWT secret read from {}", jwtSecretFileName);
            return jwtSecretFromFile;
        } catch (IOException e) {
            log.info("No JWT secret found in {}", jwtSecretFileName);

            var random = new Random();
            var randomJwtSecret = new byte[256];
            random.nextBytes(randomJwtSecret);
            var randomJwtSecretString = Base64.getEncoder().encodeToString(randomJwtSecret);

            try {
                Files.createDirectories(baseDirectory);
                Files.writeString(baseDirectory.resolve(jwtSecretFileName), randomJwtSecretString);
                return randomJwtSecretString;
            } catch (IOException ex) {
                log.error("Error while writing {}", jwtSecretFileName, ex);
                throw new FatalBeanException("Failed to configure DepotProperties!");
            }
        }
    }

}
