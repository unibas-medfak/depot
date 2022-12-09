package ch.unibas.medizin.depot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLogService implements LogService {

    @Override
    public void log(EventType type, String subject, String description) {
        log.debug("{} {} {} {}", ZonedDateTime.now(), type, subject, description);
    }

}
