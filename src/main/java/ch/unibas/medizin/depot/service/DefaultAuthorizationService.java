package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuthorizationService implements AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuthorizationService.class);

    private final PasswordEncoder passwordEncoder;

    private final DepotProperties depotProperties;

    public DefaultAuthorizationService(DepotProperties depotProperties) {
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.depotProperties = depotProperties;
    }

    @Override
    public boolean adminPasswordMismatches(String givenAdminPassword) {
        if (!passwordEncoder.matches(givenAdminPassword, depotProperties.getAdminPassword())) {
            log.error("Log request with invalid password");
            throw new AccessDeniedException("invalid password");
        }

        return false;
    }

}
