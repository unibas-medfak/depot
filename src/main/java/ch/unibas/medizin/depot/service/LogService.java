package ch.unibas.medizin.depot.service;

public interface LogService {

    enum EventType {
        GET, PUT, LIST
    }

    void log(EventType type, String subject, String description);
}
