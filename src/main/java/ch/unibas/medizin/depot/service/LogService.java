package ch.unibas.medizin.depot.service;

public interface LogService {

    enum EventType {
        TOKEN, GET, PUT, LIST
    }

    void log(EventType type, String subject, String description);
}
