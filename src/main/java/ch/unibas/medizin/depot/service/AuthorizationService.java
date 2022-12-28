package ch.unibas.medizin.depot.service;

public interface AuthorizationService {
    boolean adminPasswordMismatches(String givenAdminPassword);
}
