package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.LogRequestDto;
import ch.unibas.medizin.depot.util.DepotUtil;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final DepotProperties depotProperties;

    private final AuthorizationService authorizationService;

    public AdminService(DepotProperties depotProperties, AuthorizationService authorizationService) {
        this.depotProperties = depotProperties;
        this.authorizationService = authorizationService;
    }

    public List<String> getLastLogLines(LogRequestDto logRequestDto) {
        if (authorizationService.adminPasswordMismatches(logRequestDto.password())) {
            return List.of();
        }

        log.info("Log requested");

        var lastLogLines  = new ArrayList<String>();

        var maxNumberOfLinesToRead = 100;
        var logfile = depotProperties.getBaseDirectory().resolve(DepotUtil.LOGFILE_NAME).toString();

        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(logfile), StandardCharsets.UTF_8)) {
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
