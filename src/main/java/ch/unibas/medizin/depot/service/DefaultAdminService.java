package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.LogRequestDto;
import ch.unibas.medizin.depot.util.DepotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAdminService implements AdminService {

    private final DepotProperties depotProperties;

    private final PasswordEncoder passwordEncoder;

    @Override
    public List<String> getLastLogLines(LogRequestDto logRequestDto) {
        if (!passwordEncoder.matches(logRequestDto.password(), depotProperties.adminPassword())) {
            log.error("Log request with invalid password");
            throw new AccessDeniedException("invalid password");
        }

        log.info("Log requested");

        var lastLogLines  = new ArrayList<String>();

        var maxNumberOfLinesToRead = 100;
        var logfile = depotProperties.baseDirectory().resolve(DepotUtil.LOGFILE_NAME).toString();

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
