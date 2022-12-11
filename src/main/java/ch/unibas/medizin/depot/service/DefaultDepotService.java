package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.FileDto;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import ch.unibas.medizin.depot.util.DepotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDepotService implements DepotService {

    private final DepotProperties depotProperties;

    private final LogService logService;

    @PostConstruct
    private void init() {
        try {
            Files.createDirectories(depotProperties.baseDirectory());
        } catch (IOException e) {
            log.error("Error", e);
            throw new RuntimeException("Could not initialize base directory");
        }
    }

    @Override
    public List<FileDto> list(String path) {
        var subjectAndBasePath = getBasePathAndSubject();
        var normalizedPath = DepotUtil.normalizePath(path);
        var fullPath = subjectAndBasePath.basePath().resolve(normalizedPath);

        logService.log(LogService.EventType.LIST, subjectAndBasePath.subject(), fullPath.toString());
        log.info("{} list {}", subjectAndBasePath.subject(), fullPath);

        var entries = new LinkedList<FileDto>();

        try (var fileList = Files.list(fullPath)) {
            fileList.forEach(entry -> entries.add(new FileDto(entry.getFileName().toString(), Files.isDirectory(entry) ? FileDto.FileType.FOLDER : FileDto.FileType.FILE)));
        } catch (IOException e) {
            log.info("No such path {}", fullPath);
        }

        return entries;
    }

    @Override
    public Resource get(String file) {
        var basePathAndSubject = getBasePathAndSubject();
        var normalizedFile = DepotUtil.normalizePath(file);
        var fullPath = basePathAndSubject.basePath().resolve(normalizedFile);
        var resource = new FileSystemResource(fullPath);

        logService.log(LogService.EventType.GET, basePathAndSubject.subject(), fullPath.toString());
        log.info("{} get {}", basePathAndSubject.subject(), fullPath);

        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Could not read the file!");
        }
    }

    @Override
    public PutFileResponseDto put(MultipartFile file, String path, boolean hash) {
        var basePathAndSubject = getBasePathAndSubject();
        var normalizedPath = DepotUtil.normalizePath(path);
        var fullPath = basePathAndSubject.basePath().resolve(normalizedPath);
        var fullPathAndFile = fullPath.resolve(Objects.requireNonNull(file.getOriginalFilename()));

        logService.log(LogService.EventType.PUT, basePathAndSubject.subject(), fullPathAndFile.toString());
        log.info("{} put {}", basePathAndSubject.subject(), fullPathAndFile);

        try {
            Files.createDirectories(fullPath);
        } catch (IOException e) {
            log.error("Could not initialize folder for upload", e);
            throw new RuntimeException("Could not initialize folder for upload!");
        }

        try {
            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            var bytes = Files.copy(file.getInputStream(), fullPathAndFile, options);

            if (hash) {
                return new PutFileResponseDto(bytes, DigestUtils.sha256Hex(file.getInputStream()));
            }

            return new PutFileResponseDto(bytes, "-");
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    private record BasePathAndSubject(Path basePath, String subject) {
    }

    private BasePathAndSubject getBasePathAndSubject() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var realmAndSubject = StringUtils.split(authentication.getName(), String.valueOf(Character.LINE_SEPARATOR));
        assert realmAndSubject != null;
        var realm = realmAndSubject[0];
        var rootAndRealmPath = depotProperties.baseDirectory().resolve(realm);
        return new BasePathAndSubject(rootAndRealmPath, realmAndSubject[1]);
    }

}
