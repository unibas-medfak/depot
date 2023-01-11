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

    enum EventType {
        TOKEN, GET, PUT, LIST
    }

    private final Logger log;

    public LogService(DepotProperties depotProperties) {
        var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setPattern("%date %msg%n");
        patternLayoutEncoder.setContext(loggerContext);
        patternLayoutEncoder.start();

        var fileAppender = new FileAppender<ILoggingEvent>();
        var logfile = depotProperties.getBaseDirectory().resolve(DepotUtil.LOGFILE_NAME).toString();
        fileAppender.setFile(logfile);
        fileAppender.setEncoder(patternLayoutEncoder);
        fileAppender.setContext(loggerContext);
        fileAppender.start();

        var logger = loggerContext.getLogger(LogService.class.getName());
        logger.setAdditive(false);
        logger.setLevel(Level.INFO);
        logger.addAppender(fileAppender);

        this.log = logger;
    }

    public void log(EventType type, String subject, String description) {
        log.info("{} {} {}", type, subject, description);
    }

}
