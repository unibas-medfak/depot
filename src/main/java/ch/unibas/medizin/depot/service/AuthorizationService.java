package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public record AuthorizationService(
        PasswordEncoder passwordEncoder,
        DepotProperties depotProperties
) {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    public void throwIfAdminPasswordMismatches(final @Nonnull String givenTenant, final @Nonnull String givenAdminPassword) {

        var tenant = depotProperties.getTenants().get(givenTenant);

        if (tenant == null) {
            log.error("Request with invalid tenant");
            throw new AccessDeniedException("Invalid credentials");
        }

        if (!StringUtils.hasText(tenant.password())) {
            log.error("Tenant password is empty");
            throw new AccessDeniedException("Invalid credentials");
        }

        if (!passwordEncoder.matches(givenAdminPassword, tenant.password())) {
            log.error("Request with invalid admin password");
            throw new AccessDeniedException("Invalid credentials");
        }
    }

}
