package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.FileDto;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import ch.unibas.medizin.depot.exception.FileAlreadyExistsAsFolderException;
import ch.unibas.medizin.depot.exception.FileNotFoundException;
import ch.unibas.medizin.depot.exception.FolderAlreadyExistsAsFileException;
import ch.unibas.medizin.depot.exception.PathNotFoundException;
import ch.unibas.medizin.depot.security.JWTAuthorizationFilter;
import ch.unibas.medizin.depot.util.DepotUtil;
import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.digest.DigestUtils;
import org.jspecify.annotations.NullMarked;
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
import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Service
@NullMarked
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
        final var tokenData = getTokenData();
        final var normalizedPath = DepotUtil.normalizePath(path);
        final var fullPath = tokenData.basePath().resolve(normalizedPath);

        logService.log(tokenData.tenant, LogService.EventType.LIST, tokenData.subject(), fullPath.toString());
        log.info("{} list {}", tokenData.subject(), fullPath);

        final var entries = new ArrayList<FileDto>();

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
        final var tokenData = getTokenData();
        final var fullPath = tokenData.basePath().resolve(normalizedFile).normalize().toAbsolutePath();
        final var basePath = tokenData.basePath().normalize().toAbsolutePath();

        // Ensure fullPath is still contained in basePath
        if (!fullPath.startsWith(basePath)) {
            log.info("Requested path {} is outside of base directory {}", fullPath, basePath);
            throw new FileNotFoundException(file); // or use a dedicated exception
        }

        logService.log(tokenData.tenant, LogService.EventType.GET, tokenData.subject(), fullPath.toString());
        log.info("{} get {}", tokenData.subject(), fullPath);

        if (Files.isDirectory(fullPath)) {
            log.info("{} is a directory", fullPath);
            throw new FileNotFoundException(file);
        }

        final var resource = new FileSystemResource(fullPath);

        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new FileNotFoundException(file);
        }
    }

    public PutFileResponseDto put(final MultipartFile file, final String path, final boolean hash) {
        final var tokenData = getTokenData();
        final var normalizedPath = DepotUtil.normalizePath(path);
        final var fullPath = tokenData.basePath().resolve(normalizedPath).normalize().toAbsolutePath();
        final var fullPathAndFile = fullPath.resolve(Objects.requireNonNull(file.getOriginalFilename())).normalize().toAbsolutePath();

        // Ensure the resolved file stays within the tenant/base path
        if (!fullPathAndFile.startsWith(fullPath + File.separator)) {
            log.error("Attempt to store file outside permitted path: {}", fullPathAndFile);
            throw new IllegalArgumentException("Invalid file path");
        }

        logService.log(tokenData.tenant, LogService.EventType.PUT, tokenData.subject(), fullPathAndFile.toString());
        log.info("{} put {}", tokenData.subject(), fullPathAndFile);

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
            CopyOption[] options = {StandardCopyOption.REPLACE_EXISTING};

            final var bytes = Files.copy(file.getInputStream(), tmpFile, options);

            // Rename old file if it exists and has different content
            renameOldFileIfDifferent(fullPathAndFile, tmpFile);

            Files.move(tmpFile, fullPathAndFile, StandardCopyOption.ATOMIC_MOVE);

            return new PutFileResponseDto(bytes, hash ? DigestUtils.sha256Hex(file.getInputStream()) : "-");
        } catch (Exception e) {
            log.error("Could not store the file", e);
            throw new RuntimeException("Could not store the file. " + e.getMessage());
        }
    }

    private void renameOldFileIfDifferent(final Path targetPath, final Path newFilePath) throws IOException {
        // Synchronize on the canonical path to prevent race conditions with concurrent writes to the same file
        synchronized (targetPath.toAbsolutePath().toString().intern()) {
            if (!Files.exists(targetPath)) {
                return;
            }

            // Check if the existing file has different content than the new file
            if (filesHaveDifferentContent(targetPath, newFilePath)) {
                final var renamedPath = getNextAvailablePathWithCount(targetPath);
                log.info("Renaming existing file {} to {}", targetPath, renamedPath);
                Files.move(targetPath, renamedPath, StandardCopyOption.ATOMIC_MOVE);
            }
        }
    }

    private boolean filesHaveDifferentContent(final Path file1, final Path file2) throws IOException {
        // Compare file sizes first for quick check
        if (Files.size(file1) != Files.size(file2)) {
            return true;
        }

        // Compare content byte by byte
        return !Arrays.equals(Files.readAllBytes(file1), Files.readAllBytes(file2));
    }

    private Path getNextAvailablePathWithCount(final Path originalPath) {
        final var parent = originalPath.getParent();
        final var fileName = originalPath.getFileName().toString();

        // Split filename into name and extension
        final var lastDotIndex = fileName.lastIndexOf('.');
        final String nameWithoutExtension;
        final String extension;

        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            nameWithoutExtension = fileName.substring(0, lastDotIndex);
            extension = fileName.substring(lastDotIndex);
        } else {
            nameWithoutExtension = fileName;
            extension = "";
        }

        // Find next available count
        int count = 1;
        Path newPath;
        do {
            final var newFileName = nameWithoutExtension + "_" + count + extension;
            newPath = parent.resolve(newFileName);
            count++;
        } while (Files.exists(newPath));

        return newPath;
    }

    public void delete(final String path) {
        final var normalizedFile = DepotUtil.normalizePath(path);
        final var tokenData = getTokenData();
        final var fullPath = tokenData.basePath().resolve(normalizedFile);

        try {
            FileSystemUtils.deleteRecursively(fullPath);
        } catch (IOException e) {
            log.error("Could not delete file or folder", e);
            throw new RuntimeException("Could not delete file or folder.");
        }

        logService.log(tokenData.tenant, LogService.EventType.DELETE, tokenData.subject(), fullPath.toString());
        log.info("{} delete {}", tokenData.subject(), fullPath);
    }

    private record TokenData(String tenant, Path basePath, String subject) {
    }

    private TokenData getTokenData() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final var tenantRealmAndSubject = Arrays.asList(authentication.getName().split(JWTAuthorizationFilter.TOKEN_DATA_DELIMITER));
        assert tenantRealmAndSubject.size() == 3;

        final var tenant = tenantRealmAndSubject.get(0);
        assert StringUtils.hasText(tenant);

        final var realm = tenantRealmAndSubject.get(1);
        assert StringUtils.hasText(realm);

        final var subject = tenantRealmAndSubject.get(2);
        assert StringUtils.hasText(subject);

        final var rootAndRealmPath = depotProperties.getBaseDirectory().resolve(tenant).resolve(realm);
        return new TokenData(tenant, rootAndRealmPath, subject);
    }

}
