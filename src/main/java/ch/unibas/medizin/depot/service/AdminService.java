package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.LogRequestDto;
import ch.unibas.medizin.depot.util.DepotUtil;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@NullMarked
public record AdminService(
        DepotProperties depotProperties,
        AuthorizationService authorizationService
) {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    public List<String> getLastLogLines(final LogRequestDto logRequestDto) {
        authorizationService.throwIfAdminPasswordMismatches(logRequestDto.tenant(), logRequestDto.password());

        log.info("Log requested");

        final var lastLogLines = new ArrayList<String>();

        final var maxNumberOfLinesToRead = 100;
        final var logfile = depotProperties.getBaseDirectory().resolve(logRequestDto.tenant()).resolve(DepotUtil.LOGFILE_NAME).toString();

        try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder().setFile(new File(logfile)).setCharset(StandardCharsets.UTF_8).get()) {
            String line;
            while ((line = reader.readLine()) != null && lastLogLines.size() < maxNumberOfLinesToRead) {
                lastLogLines.add(line);
            }

        } catch (IOException e) {
            log.error("Error while read log", e);
        }

        return lastLogLines;
    }

}
