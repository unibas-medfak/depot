package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.FileDto;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import ch.unibas.medizin.depot.exception.DestinationAlreadyExistsException;
import ch.unibas.medizin.depot.exception.FileAlreadyExistsAsFolderException;
import ch.unibas.medizin.depot.exception.FileNotFoundException;
import ch.unibas.medizin.depot.exception.FolderAlreadyExistsAsFileException;
import ch.unibas.medizin.depot.exception.PathNotFoundException;
import ch.unibas.medizin.depot.security.JWTAuthorizationFilter;
import ch.unibas.medizin.depot.util.DepotUtil;
import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.logging.log4j.util.Strings;
import org.apache.tika.Tika;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@NullMarked
public record DepotService(
        DepotProperties depotProperties,
        LogService logService
) {

    private static final Logger log = LoggerFactory.getLogger(DepotService.class);

    private static final Tika TIKA = new Tika();

    @PostConstruct
    private void init() {
        try {
            Files.createDirectories(depotProperties.getBaseDirectory().resolve("tmp"));
        } catch (IOException e) {
            log.error("Could not create base directory {}", depotProperties.getBaseDirectory(), e);
            throw new RuntimeException("Could not initialize base directory");
        }
    }

    public List<FileDto> list(final String path, final boolean hash) {
        final var tokenData = getTokenData();
        final var normalizedPath = DepotUtil.normalizePath(path);
        final var fullPath = tokenData.basePath().resolve(normalizedPath);

        final var entries = new ArrayList<FileDto>();

        try (final var fileList = Files.list(fullPath)) {
            fileList.filter(entry -> !entry.getFileName().toString().startsWith(".")).forEach(entry -> {
                try {
                    final var basicFileAttributes = Files.readAttributes(entry, BasicFileAttributes.class);
                    final var isDirectory = Files.isDirectory(entry);
                    String hashValue = Strings.EMPTY;
                    if (hash && !isDirectory) {
                        final var hash128 = MurmurHash3.hash128x64(Files.readAllBytes(entry));
                        hashValue = String.format("%016x%016x", hash128[0], hash128[1]);
                    }
                    entries.add(
                            new FileDto(
                                    entry.getFileName().toString(),
                                    isDirectory ? FileDto.FileType.FOLDER : FileDto.FileType.FILE,
                                    isDirectory ? 0 : basicFileAttributes.size(),
                                    basicFileAttributes.lastModifiedTime().toInstant(),
                                    hashValue
                            )
                    );
                } catch (IOException e) {
                    log.error("Could not read file attributes {}", kv("path", entry.toString()), e);
                }
            });
        } catch (IOException e) {
            if (!normalizedPath.toString().isBlank()) {
                log.info("List path not found {}", kv("path", fullPath.toString()));
                throw new PathNotFoundException(path);
            }
            // Realm root not created yet (nothing stored so far): a valid, empty listing
        }

        logService.log(tokenData.tenant, LogService.EventType.LIST, tokenData.subject(), fullPath.toString());
        log.info("Listed {}", kv("path", fullPath.toString()));

        return entries;
    }

    public ResponseEntity<Resource> get(final String file) {
        final var normalizedFile = DepotUtil.normalizePath(file);
        final var tokenData = getTokenData();
        final var fullPath = tokenData.basePath().resolve(normalizedFile).normalize().toAbsolutePath();
        final var basePath = tokenData.basePath().normalize().toAbsolutePath();

        // Ensure fullPath is still contained in basePath
        if (!fullPath.startsWith(basePath)) {
            log.warn("Rejected get outside tenant directory {}", kv("path", fullPath.toString()));
            throw new FileNotFoundException(file); // or use a dedicated exception
        }

        if (Files.isDirectory(fullPath)) {
            log.info("Rejected get of a folder {}", kv("path", fullPath.toString()));
            throw new FileNotFoundException(file);
        }

        final var resource = new FileSystemResource(fullPath);

        if (!resource.exists() || !resource.isReadable()) {
            log.info("File not found or not readable {}", kv("path", fullPath.toString()));
            throw new FileNotFoundException(file);
        }

        logService.log(tokenData.tenant, LogService.EventType.GET, tokenData.subject(), fullPath.toString());
        log.info("Served {}", kv("path", fullPath.toString()));

        String contentType;
        try {
            contentType = TIKA.detect(fullPath);
        } catch (IOException e) {
            log.debug("Could not detect content type {}", kv("path", fullPath.toString()));
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
    }

    public PutFileResponseDto put(final MultipartFile file, final String path, final boolean hash) {
        final var tokenData = getTokenData();
        final var filename = Objects.requireNonNull(file.getOriginalFilename());
        final var target = prepareTarget(tokenData, path, filename);

        Path tmpFile = null;
        try {
            tmpFile = Files.createTempFile(depotProperties.getBaseDirectory().resolve("tmp"), "depot", "");
            final var bytes = Files.copy(file.getInputStream(), tmpFile, StandardCopyOption.REPLACE_EXISTING);
            final var response = store(tokenData, target, tmpFile, bytes, hash);
            tmpFile = null; // Successfully moved, don't clean up in finally
            return response;
        } catch (Exception e) {
            log.error("Could not store file {}", kv("path", target.fullPathAndFile().toString()), e);
            throw new RuntimeException("Could not store the file. " + e.getMessage());
        } finally {
            // Clean up temp file if still exists (upload failed)
            if (tmpFile != null) {
                try {
                    Files.deleteIfExists(tmpFile);
                    log.debug("Deleted temporary file {}", kv("path", tmpFile.toString()));
                } catch (IOException e) {
                    log.error("Could not delete temporary file {}", kv("path", tmpFile.toString()), e);
                }
            }
        }
    }

    /**
     * Throws if a file with the given name cannot be stored in the given path, without storing anything.
     */
    public void checkPutTarget(final String path, final String filename) {
        prepareTarget(getTokenData(), path, filename);
    }

    /**
     * Stores an already complete file like {@link #put}. On success the temp file is moved into place;
     * on failure it is left untouched and remains the caller's to clean up.
     */
    public PutFileResponseDto putFromFile(final Path tmpFile, final String path, final String filename, final boolean hash) {
        final var tokenData = getTokenData();
        final var target = prepareTarget(tokenData, path, filename);

        try {
            return store(tokenData, target, tmpFile, Files.size(tmpFile), hash);
        } catch (Exception e) {
            log.error("Could not store file {}", kv("path", target.fullPathAndFile().toString()), e);
            throw new RuntimeException("Could not store the file. " + e.getMessage());
        }
    }

    private record PutTarget(Path fullPath, Path fullPathAndFile) {
    }

    private PutTarget prepareTarget(final TokenData tokenData, final String path, final String filename) {
        final var normalizedPath = DepotUtil.normalizePath(path);
        final var basePath = tokenData.basePath().normalize().toAbsolutePath();
        final var fullPath = tokenData.basePath().resolve(normalizedPath).normalize().toAbsolutePath();
        final var fullPathAndFile = fullPath.resolve(filename).normalize().toAbsolutePath();

        // Ensure the resolved file stays within the tenant base
        if (!fullPathAndFile.startsWith(basePath)) {
            log.warn("Rejected put outside tenant directory {}", kv("path", fullPathAndFile.toString()));
            throw new IllegalArgumentException("Invalid file path");
        }

        if (Files.isRegularFile(fullPath)) {
            log.info("Rejected put because folder exists as a file {}", kv("path", fullPath.toString()));
            throw new FolderAlreadyExistsAsFileException(path);
        }

        if (Files.isDirectory(fullPathAndFile)) {
            log.info("Rejected put because file exists as a folder {}", kv("path", fullPathAndFile.toString()));
            throw new FileAlreadyExistsAsFolderException(filename);
        }

        try {
            Files.createDirectories(fullPath);
        } catch (FileAlreadyExistsException e) {
            log.debug("Folder already exists {}", kv("path", fullPath.toString()));
        } catch (IOException e) {
            log.error("Could not create upload folder {}", kv("path", fullPath.toString()), e);
            throw new RuntimeException("Could not initialize folder for upload.");
        }

        return new PutTarget(fullPath, fullPathAndFile);
    }

    private PutFileResponseDto store(final TokenData tokenData, final PutTarget target, final Path tmpFile, final long bytes, final boolean hash) throws IOException {
        final var fullPath = target.fullPath();
        final var fullPathAndFile = target.fullPathAndFile();

        final var tenantConfig = depotProperties.getTenants().get(tokenData.tenant());
        final var backup = tenantConfig != null && tenantConfig.backup();

        final var backupExisting = backup && Files.exists(fullPathAndFile);

        // Only read the file back and hash it when the result is actually needed:
        // either the caller requested a hash, or a backup overwrite has to compare content.
        var hashValue = "-";
        if (hash || backupExisting) {
            final var hash128 = MurmurHash3.hash128x64(Files.readAllBytes(tmpFile));

            if (hash) {
                hashValue = String.format("%016x%016x", hash128[0], hash128[1]);
            }

            if (backupExisting) {
                final var existingHash128 = MurmurHash3.hash128x64(Files.readAllBytes(fullPathAndFile));
                if (!Arrays.equals(hash128, existingHash128)) {
                    final var fileName = fullPathAndFile.getFileName().toString();
                    final var backupDir = fullPath.resolve("." + fileName);
                    Files.createDirectories(backupDir);
                    final long existingBackups;
                    try (var backups = Files.list(backupDir)) {
                        existingBackups = backups.count();
                    }
                    final var backupFile = backupDir.resolve(fileName + "_" + (existingBackups + 1));
                    Files.move(fullPathAndFile, backupFile, StandardCopyOption.ATOMIC_MOVE);
                    log.info("Backed up {} {}", kv("path", fullPathAndFile.toString()), kv("target", backupFile.toString()));
                }
            }
        }

        Files.move(tmpFile, fullPathAndFile, StandardCopyOption.ATOMIC_MOVE);

        logService.log(tokenData.tenant, LogService.EventType.PUT, tokenData.subject(), fullPathAndFile.toString());
        log.info("Stored {} {}", kv("path", fullPathAndFile.toString()), kv("bytes", bytes));

        return new PutFileResponseDto(bytes, hashValue);
    }

    public void move(final String fromPath, final String toPath) {
        final var tokenData = getTokenData();
        final var basePath = tokenData.basePath().normalize().toAbsolutePath();
        final var fullFromPath = tokenData.basePath().resolve(DepotUtil.normalizePath(fromPath)).normalize().toAbsolutePath();
        final var fullToPath = tokenData.basePath().resolve(DepotUtil.normalizePath(toPath)).normalize().toAbsolutePath();

        if (!fullFromPath.startsWith(basePath) || !fullToPath.startsWith(basePath)) {
            log.warn("Rejected move outside tenant directory {} {}", kv("from", fullFromPath.toString()), kv("to", fullToPath.toString()));
            throw new PathNotFoundException(fromPath);
        }

        if (fullFromPath.equals(basePath) || fullToPath.equals(basePath)) {
            log.warn("Rejected move of tenant directory {} {}", kv("from", fullFromPath.toString()), kv("to", fullToPath.toString()));
            throw new PathNotFoundException(fromPath);
        }

        if (!Files.exists(fullFromPath)) {
            log.info("Move source not found {}", kv("from", fullFromPath.toString()));
            throw new PathNotFoundException(fromPath);
        }

        if (Files.exists(fullToPath)) {
            log.info("Move destination already exists {}", kv("to", fullToPath.toString()));
            throw new DestinationAlreadyExistsException(toPath);
        }

        try {
            final var parent = fullToPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.move(fullFromPath, fullToPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Could not move {} {}", kv("from", fullFromPath.toString()), kv("to", fullToPath.toString()), e);
            throw new RuntimeException("Could not move file or folder.");
        }

        logService.log(tokenData.tenant, LogService.EventType.MOVE, tokenData.subject(), fullFromPath + " -> " + fullToPath);
        log.info("Moved {} {}", kv("from", fullFromPath.toString()), kv("to", fullToPath.toString()));
    }

    public void delete(final String path) {
        final var normalizedFile = DepotUtil.normalizePath(path);
        final var tokenData = getTokenData();
        final var fullPath = tokenData.basePath().resolve(normalizedFile);

        final var tenantConfig = depotProperties.getTenants().get(tokenData.tenant());
        final var softDelete = tenantConfig != null && tenantConfig.softDelete();

        if (!Files.exists(fullPath)) {
            log.info("Delete path not found {}", kv("path", fullPath.toString()));
            return;
        }

        try {
            if (softDelete) {
                final var target = softDelete(fullPath);
                log.info("Soft deleted {} {}", kv("path", fullPath.toString()), kv("target", target.toString()));
            } else {
                FileSystemUtils.deleteRecursively(fullPath);
                log.info("Deleted {}", kv("path", fullPath.toString()));
            }
        } catch (IOException e) {
            log.error("Could not delete {}", kv("path", fullPath.toString()), e);
            throw new RuntimeException("Could not delete file or folder.");
        }

        logService.log(tokenData.tenant, LogService.EventType.DELETE, tokenData.subject(), fullPath.toString());
    }

    private Path softDelete(final Path fullPath) throws IOException {
        final var parent = fullPath.getParent();
        if (parent == null) {
            throw new IOException("Cannot soft delete root path: " + fullPath);
        }
        final var name = fullPath.getFileName().toString();
        var target = parent.resolve("." + name);
        var counter = 1;
        while (Files.exists(target)) {
            target = parent.resolve("." + name + "_" + counter++);
        }
        Files.move(fullPath, target, StandardCopyOption.ATOMIC_MOVE);
        return target;
    }

    private record TokenData(String tenant, Path basePath, String subject) {
    }

    private TokenData getTokenData() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.error("No authentication in security context");
            throw new IllegalStateException("Not authenticated");
        }

        // Keep trailing empty parts (limit -1) so an injected delimiter yields the wrong part
        // count and is rejected below, rather than silently truncating the subject.
        final var tenantRealmAndSubject = Arrays.asList(
                authentication.getName().split(Pattern.quote(JWTAuthorizationFilter.TOKEN_DATA_DELIMITER), -1));
        if (tenantRealmAndSubject.size() != 3) {
            log.warn("Rejected malformed principal {}", kv("parts", tenantRealmAndSubject.size()));
            throw new IllegalStateException("Illegal security context");
        }

        final var tenant = tenantRealmAndSubject.get(0);
        if (!StringUtils.hasText(tenant)) {
            log.warn("Rejected principal with empty tenant");
            throw new IllegalStateException("Empty tenant");
        }
        if (!DepotUtil.isValidTenantOrRealm(tenant)) {
            log.warn("Rejected principal with illegal tenant {}", tenant);
            throw new AccessDeniedException("Illegal tenant");
        }

        final var realm = tenantRealmAndSubject.get(1);
        if (!StringUtils.hasText(realm)) {
            log.warn("Rejected principal with empty realm");
            throw new IllegalStateException("Empty realm");
        }
        if (!DepotUtil.isValidTenantOrRealm(realm)) {
            log.warn("Rejected principal with illegal realm {}", realm);
            throw new AccessDeniedException("Illegal realm");
        }

        final var subject = tenantRealmAndSubject.get(2);
        if (!StringUtils.hasText(subject)) {
            log.warn("Rejected principal with empty subject");
            throw new IllegalStateException("Empty subject");
        }

        final var rootAndRealmPath = depotProperties.getBaseDirectory().resolve(tenant).resolve(realm);
        return new TokenData(tenant, rootAndRealmPath, subject);
    }

}
