package ch.unibas.medizin.depot.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

@Validated
@ConfigurationProperties(prefix = "depot")
public class DepotProperties {

    private static final Logger log = LoggerFactory.getLogger(DepotProperties.class);

    @NotNull
    private final Path baseDirectory;

    @NotEmpty
    private final String host;

    @NotEmpty
    private final String timeZone;

    @NotEmpty
    private final String jwtSecret;

    @NotEmpty
    private final Map<String, Tenant> tenants;

    @SuppressWarnings("unused")
    public record Tenant(@NotEmpty String password) {
    }

    public DepotProperties(Path baseDirectory, String host, String timeZone, String jwtSecret, Map<String, Tenant> tenants) {
        if ("Mac OS X".equals(SystemUtils.OS_NAME)) {
            this.baseDirectory = Path.of("/tmp/depot");
        }
        else {
            this.baseDirectory = baseDirectory;
        }

        this.host = host;
        this.timeZone = timeZone;
        this.jwtSecret = getJwtSecret(this.baseDirectory, jwtSecret);
        this.tenants = tenants;
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

    public Map<String, Tenant> getTenants() {
        return tenants;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    private String getJwtSecret(Path baseDirectory, String jwtSecretFromProperties) {
        if (StringUtils.hasText(jwtSecretFromProperties)) {
            return jwtSecretFromProperties;
        }

        log.info("No JWT secret found in application.properties");

        var jwtSecretFileName = ".jwtsecret";
        var jwtSecretFilePath = baseDirectory.resolve(jwtSecretFileName);

        try {
            var jwtSecretFromFile = Files.readString(jwtSecretFilePath);
            log.info("JWT secret read from {}", jwtSecretFilePath);
            return jwtSecretFromFile;
        } catch (IOException e) {
            log.info("No JWT secret found in {}", jwtSecretFilePath);

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
