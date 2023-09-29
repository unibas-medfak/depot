package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public record AuthorizationService(
        PasswordEncoder passwordEncoder,
        DepotProperties depotProperties
) {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    public boolean adminPasswordMismatches(String givenAdminPassword) {
        if (!passwordEncoder.matches(givenAdminPassword, depotProperties.getAdminPassword())) {
            log.error("Log request with invalid password");
            throw new AccessDeniedException("Invalid password");
        }

        return false;
    }

}
