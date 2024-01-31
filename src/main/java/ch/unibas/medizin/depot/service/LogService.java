package ch.unibas.medizin.depot.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.util.DepotUtil;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    public enum EventType {
        TOKEN, GET, PUT, LIST, DELETE
    }

    private final Logger log;

    public LogService(DepotProperties depotProperties) {
        final var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final var patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setPattern("%date %msg%n");
        patternLayoutEncoder.setContext(loggerContext);
        patternLayoutEncoder.start();

        final var fileAppender = new FileAppender<ILoggingEvent>();
        final var logfile = depotProperties.getBaseDirectory().resolve(DepotUtil.LOGFILE_NAME).toString();
        fileAppender.setFile(logfile);
        fileAppender.setEncoder(patternLayoutEncoder);
        fileAppender.setContext(loggerContext);
        fileAppender.start();

        final var logger = loggerContext.getLogger(LogService.class.getName());
        logger.setAdditive(false);
        logger.setLevel(Level.INFO);
        logger.addAppender(fileAppender);

        this.log = logger;
    }

    public void log(final EventType type, final String subject, final String description) {
        log.info("{} {} {}", type, subject, description);
    }

}
