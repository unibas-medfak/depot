package ch.unibas.medizin.depot.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.unibas.medizin.depot.config.DepotProperties;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DefaultLogService implements LogService {

    private final Logger log;

    public DefaultLogService(DepotProperties depotProperties) {
        var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setPattern("%date %msg%n");
        patternLayoutEncoder.setContext(loggerContext);
        patternLayoutEncoder.start();

        var fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setFile(depotProperties.baseDirectory() + File.separator + "depot-access.log");
        fileAppender.setEncoder(patternLayoutEncoder);
        fileAppender.setContext(loggerContext);
        fileAppender.start();

        var logger = loggerContext.getLogger(DefaultLogService.class.getName());
        logger.setAdditive(false);
        logger.setLevel(Level.INFO);
        logger.addAppender(fileAppender);

        this.log = logger;
    }

    @Override
    public void log(EventType type, String subject, String description) {
        log.info("{} {} {}", type, subject, description);
    }

}
