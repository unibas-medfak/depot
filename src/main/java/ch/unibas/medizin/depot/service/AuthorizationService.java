package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@NullMarked
public record AuthorizationService(
        PasswordEncoder passwordEncoder,
        DepotProperties depotProperties,
        LogService logService
) {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    public void throwIfAdminPasswordMismatches(final String givenTenant, final String givenAdminPassword) {

        var tenant = depotProperties.getTenants().get(givenTenant);

        if (tenant == null) {
            log.warn("Token request for unknown tenant {} {}", kv("tenant", givenTenant), kv("client", clientAddress()));
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!StringUtils.hasText(tenant.password())) {
            log.error("Token request for tenant without password {} {}", kv("tenant", givenTenant), kv("client", clientAddress()));
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(givenAdminPassword, tenant.password())) {
            final var clientAddress = clientAddress();
            log.warn("Token request with wrong admin password {} {}", kv("tenant", givenTenant), kv("client", clientAddress));
            logService.log(givenTenant, LogService.EventType.TOKEN_DENIED, "-", "invalid admin password from " + clientAddress);
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    private static String clientAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return "unknown";
    }

}
