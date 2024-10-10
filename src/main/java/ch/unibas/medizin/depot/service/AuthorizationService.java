package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.security.JWTAuthorizationFilter;
import ch.unibas.medizin.depot.security.TokenData;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Service
public record AuthorizationService(
        PasswordEncoder passwordEncoder,
        DepotProperties depotProperties
) {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    public TokenData getTokenData() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final var tenantRealmAndSubject = Arrays.asList(authentication.getName().split(JWTAuthorizationFilter.TOKEN_DATA_DELIMITER));
        assert tenantRealmAndSubject.size() == 3;

        final var tenant = tenantRealmAndSubject.get(0);
        assert StringUtils.hasText(tenant);

        final var realm = tenantRealmAndSubject.get(1);
        assert StringUtils.hasText(realm);

        final var subject = tenantRealmAndSubject.get(2);
        assert StringUtils.hasText(subject);

        return getTokenData(tenant, realm, subject);
    }

    public TokenData getTokenData(final @Nonnull String tenant, final @Nonnull String realm, final @Nonnull String subject) {
        final var rootAndRealmPath = depotProperties.getBaseDirectory().resolve(tenant).resolve(realm);
        return new TokenData(tenant, rootAndRealmPath, subject);
    }

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
