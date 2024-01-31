package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.FileDto;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import ch.unibas.medizin.depot.exception.FileAlreadyExistsAsFolderException;
import ch.unibas.medizin.depot.exception.FileNotFoundException;
import ch.unibas.medizin.depot.exception.FolderAlreadyExistsAsFileException;
import ch.unibas.medizin.depot.exception.PathNotFoundException;
import ch.unibas.medizin.depot.util.DepotUtil;
import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Service
public record DepotService(
        DepotProperties depotProperties,
        LogService logService
) {

    private static final Logger log = LoggerFactory.getLogger(DepotService.class);

    @PostConstruct
    private void init() {
        try {
            Files.createDirectories(depotProperties.getBaseDirectory().resolve("tmp"));
        } catch (IOException e) {
            log.error("Could not initialize base directory", e);
            throw new RuntimeException("Could not initialize base directory");
        }
    }

    public List<FileDto> list(final String path) {
        final var subjectAndBasePath = getBasePathAndSubject();
        final var normalizedPath = DepotUtil.normalizePath(path);
        final var fullPath = subjectAndBasePath.basePath().resolve(normalizedPath);

        logService.log(LogService.EventType.LIST, subjectAndBasePath.subject(), fullPath.toString());
        log.info("{} list {}", subjectAndBasePath.subject(), fullPath);

        final var entries = new LinkedList<FileDto>();

        try (final var fileList = Files.list(fullPath)) {
            fileList.forEach(entry -> {
                try {
                    final var basicFileAttributes = Files.readAttributes(entry, BasicFileAttributes.class);
                    entries.add(
                            new FileDto(
                                    entry.getFileName().toString(),
                                    Files.isDirectory(entry) ? FileDto.FileType.FOLDER : FileDto.FileType.FILE,
                                    Files.isDirectory(entry) ? 0 : basicFileAttributes.size(),
                                    basicFileAttributes.lastModifiedTime().toInstant()
                            )
                    );
                } catch (IOException e) {
                    log.error("Could not read attributes of {}", fullPath);
                }
            });
        } catch (IOException e) {
            if (normalizedPath.toString().isBlank()) {
                return List.of();
            }
            log.info("No such path {}", fullPath);
            throw new PathNotFoundException(path);
        }

        return entries;
    }

    public Resource get(final String file) {
        final var normalizedFile = DepotUtil.normalizePath(file);
        final var basePathAndSubject = getBasePathAndSubject();
        final var fullPath = basePathAndSubject.basePath().resolve(normalizedFile);
        final var resource = new FileSystemResource(fullPath);

        logService.log(LogService.EventType.GET, basePathAndSubject.subject(), fullPath.toString());
        log.info("{} get {}", basePathAndSubject.subject(), fullPath);

        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new FileNotFoundException(file);
        }
    }

    public PutFileResponseDto put(final MultipartFile file, final String path, final boolean hash) {
        final var basePathAndSubject = getBasePathAndSubject();
        final var normalizedPath = DepotUtil.normalizePath(path);
        final var fullPath = basePathAndSubject.basePath().resolve(normalizedPath);
        final var fullPathAndFile = fullPath.resolve(Objects.requireNonNull(file.getOriginalFilename()));

        logService.log(LogService.EventType.PUT, basePathAndSubject.subject(), fullPathAndFile.toString());
        log.info("{} put {}", basePathAndSubject.subject(), fullPathAndFile);

        if (Files.isRegularFile(fullPath)) {
            log.error("Folder {} already exists as file", fullPath);
            throw new FolderAlreadyExistsAsFileException(path);
        }

        if (Files.isDirectory(fullPathAndFile)) {
            log.error("File {} already exists as folder", fullPathAndFile);
            throw new FileAlreadyExistsAsFolderException(file.getOriginalFilename());
        }

        try {
            Files.createDirectories(fullPath);
        } catch (FileAlreadyExistsException e) {
            log.debug("Folder {} already exists", fullPath);
        } catch (IOException e) {
            log.error("Could not initialize folder for upload", e);
            throw new RuntimeException("Could not initialize folder for upload.");
        }

        try {
            final var tmpFile = Files.createTempFile(depotProperties.getBaseDirectory().resolve("tmp"), "depot", "");
            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };

            final var bytes = Files.copy(file.getInputStream(), tmpFile, options);

            Files.move(tmpFile, fullPathAndFile, StandardCopyOption.ATOMIC_MOVE);

            return new PutFileResponseDto(bytes, hash ? DigestUtils.sha256Hex(file.getInputStream()) : "-");
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. " + e.getMessage());
        }
    }

    public void delete(final String path) {
        final var normalizedFile = DepotUtil.normalizePath(path);
        final var basePathAndSubject = getBasePathAndSubject();
        final var fullPath = basePathAndSubject.basePath().resolve(normalizedFile);

        try {
            FileSystemUtils.deleteRecursively(fullPath);
        }
        catch (IOException e) {
            log.error("Could not delete file or folder", e);
            throw new RuntimeException("Could not delete file or folder.");
        }

        logService.log(LogService.EventType.DELETE, basePathAndSubject.subject(), fullPath.toString());
        log.info("{} delete {}", basePathAndSubject.subject(), fullPath);
    }

    private record BasePathAndSubject(Path basePath, String subject) {
    }

    private BasePathAndSubject getBasePathAndSubject() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final var realmAndSubject = StringUtils.split(authentication.getName(), String.valueOf(Character.LINE_SEPARATOR));
        assert realmAndSubject != null;
        final var realm = realmAndSubject[0];
        final var rootAndRealmPath = depotProperties.getBaseDirectory().resolve(realm);
        return new BasePathAndSubject(rootAndRealmPath, realmAndSubject[1]);
    }

}
