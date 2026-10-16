package ch.unibas.medizin.depot.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Validated
@ConfigurationProperties(prefix = "depot")
public class DepotProperties {

    private static final Logger log = LoggerFactory.getLogger(DepotProperties.class);

    // Excluded from the JSON log file in logback-spring.xml, so secrets only appear on the console
    public static final Marker CONSOLE_ONLY = MarkerFactory.getMarker("CONSOLE_ONLY");

    public static final String DEFAULT_TENANT_NAME = "default";

    @NotNull
    private final Path baseDirectory;

    @NotEmpty
    private final String host;

    @NotEmpty
    private final String jwtSecret;

    @NotEmpty
    private final Map<String, Tenant> tenants;

    public record Tenant(String password, boolean softDelete, boolean backup) {
    }

    public DepotProperties(Path baseDirectory, String host, String jwtSecret, Map<String, Tenant> tenants) {
        if ("Mac OS X".equals(SystemUtils.OS_NAME)) {
            this.baseDirectory = Path.of("/tmp/depot");
        } else {
            this.baseDirectory = baseDirectory;
        }

        this.host = host;
        this.jwtSecret = getJwtSecret(this.baseDirectory, jwtSecret);
        this.tenants = getTenants(tenants);
    }

    public String getHost() {
        return host;
    }

    public Path getBaseDirectory() {
        return baseDirectory;
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

        log.info("No JWT secret configured");

        var jwtSecretFileName = ".jwtsecret";
        var jwtSecretFilePath = baseDirectory.resolve(jwtSecretFileName);

        try {
            var jwtSecretFromFile = Files.readString(jwtSecretFilePath);
            log.info("Read JWT secret from {}", jwtSecretFilePath);
            return jwtSecretFromFile;
        } catch (IOException e) {
            log.info("No JWT secret file at {}, generating a new secret", jwtSecretFilePath);

            var random = new SecureRandom();
            var randomJwtSecret = new byte[256];
            random.nextBytes(randomJwtSecret);
            var randomJwtSecretString = Base64.getEncoder().encodeToString(randomJwtSecret);

            try {
                Files.createDirectories(baseDirectory);
                Files.writeString(baseDirectory.resolve(jwtSecretFileName), randomJwtSecretString);
                return randomJwtSecretString;
            } catch (IOException ex) {
                log.error("Could not write JWT secret to {}", jwtSecretFilePath, ex);
                throw new FatalBeanException("Failed to configure DepotProperties!");
            }
        }
    }

    private Map<String, Tenant> getTenants(Map<String, Tenant> tenantsFromProperties) {
        if (tenantsFromProperties != null && !tenantsFromProperties.isEmpty()) {

            var allTenantsHavePasswords = true;
            var removeDefaultTenant = false;

            for (var tenantFromProperties : tenantsFromProperties.entrySet()) {
                if (StringUtils.hasText(tenantFromProperties.getValue().password())) {
                    log.info("Configured tenant {}", tenantFromProperties.getKey());
                } else {
                    if (tenantFromProperties.getKey().equals(DEFAULT_TENANT_NAME)) {
                        removeDefaultTenant = true;
                    } else {
                        log.warn("Tenant {} has no password configured", tenantFromProperties.getKey());
                        allTenantsHavePasswords = false;
                    }
                }

            }

            if (allTenantsHavePasswords) {
                if (removeDefaultTenant) {
                    tenantsFromProperties.remove(DEFAULT_TENANT_NAME);
                }

                if (!tenantsFromProperties.isEmpty()) {
                    return tenantsFromProperties;
                }

                log.info("No tenants configured");
            } else {
                log.warn("Ignoring configured tenants because not all of them have a password, falling back to the default tenant");
            }
        } else {
            log.info("No tenants configured");
        }

        var defaultTenantPasswordFilename = ".default";
        var defaultTenantPasswordPath = baseDirectory.resolve(defaultTenantPasswordFilename);

        try {
            var encodedDefaultTenantPassword = Files.readString(defaultTenantPasswordPath);
            log.info("Read default tenant password from {}", defaultTenantPasswordPath);
            return Map.of(DEFAULT_TENANT_NAME, new Tenant(encodedDefaultTenantPassword, false, false));
        } catch (IOException e) {
            log.info("No default tenant password file at {}, generating a new password", defaultTenantPasswordPath);

            var randomPassword = UUID.randomUUID().toString();
            log.error(CONSOLE_ONLY, "!!!!!!!!!!!!!!!! DEFAULT TENANT PASSWORD = {} !!!!!!!!!!!!!!!!", randomPassword);

            var passwordEncoder = new BCryptPasswordEncoder();
            var encodedDefaultTenantPassword = passwordEncoder.encode(randomPassword);

            try {
                if (encodedDefaultTenantPassword == null) {
                    throw new IllegalStateException("Password encoding returned null");
                }
                Files.createDirectories(baseDirectory);
                Files.writeString(baseDirectory.resolve(defaultTenantPasswordFilename), encodedDefaultTenantPassword);
                return Map.of(DEFAULT_TENANT_NAME, new Tenant(encodedDefaultTenantPassword, false, false));
            } catch (IOException ex) {
                log.error("Could not write default tenant password to {}", defaultTenantPasswordPath, ex);
                throw new FatalBeanException("Failed to configure DepotProperties!");
            }
        }
    }

}
