package ch.unibas.medizin.depot.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.util.DepotUtil;
import org.jspecify.annotations.NullMarked;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@NullMarked
public class LogService {

    public enum EventType {
        TOKEN, TOKEN_DENIED, GET, PUT, LIST, DELETE, MOVE
    }

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LogService.class);

    // Prefix keeps tenant loggers out of the application's logger hierarchy, so a tenant
    // named e.g. "org" or "ch" does not swallow those packages' log output.
    private static final String AUDIT_LOGGER_PREFIX = "depot.audit.";

    private final Map<String, Logger> loggers = new HashMap<>();

    public LogService(DepotProperties depotProperties) {
        for (var tenant : depotProperties.getTenants().keySet()) {
            var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            var patternLayoutEncoder = new PatternLayoutEncoder();
            patternLayoutEncoder.setPattern("%date %msg%n");
            patternLayoutEncoder.setContext(loggerContext);
            patternLayoutEncoder.start();

            var fileAppender = new FileAppender<ILoggingEvent>();
            var logfilePath = depotProperties.getBaseDirectory().resolve(tenant);
            //noinspection ResultOfMethodCallIgnored
            logfilePath.toFile().mkdirs();
            var logfile = depotProperties.getBaseDirectory().resolve(tenant).resolve(DepotUtil.LOGFILE_NAME).toString();
            fileAppender.setFile(logfile);
            fileAppender.setEncoder(patternLayoutEncoder);
            fileAppender.setContext(loggerContext);
            fileAppender.start();

            var logger = loggerContext.getLogger(AUDIT_LOGGER_PREFIX + tenant);
            logger.setAdditive(false);
            logger.setLevel(Level.INFO);
            logger.addAppender(fileAppender);

            loggers.put(tenant, logger);
        }
    }

    public void log(final String tenant, final EventType type, final String subject, final String description) {
        var logger = loggers.get(tenant);
        if (logger == null) {
            log.warn("Dropping audit event {} for unknown tenant {}", type, tenant);
            return;
        }
        logger.info("{} {} {}", type, subject, description);
    }

}
