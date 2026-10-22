package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import ch.unibas.medizin.depot.dto.UploadStartRequestDto;
import ch.unibas.medizin.depot.dto.UploadStatusDto;
import ch.unibas.medizin.depot.exception.InvalidRequestException;
import ch.unibas.medizin.depot.service.UploadService;
import ch.unibas.medizin.depot.util.DepotUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;

import static net.logstash.logback.argument.StructuredArguments.kv;

@NullMarked
@RestController
@SecurityRequirement(name = "depotapi")
@Tag(name = "Upload", description = "Chunked, resumable upload API for large files")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final String INVALID_REQUEST_DETAIL = "must only contain letters, digits and the following chars . _ - @ + /";

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('WRITE')")
    @Operation(summary = "Start an upload; send the file in chunks of at most the returned chunkSize, then complete it")
    public ResponseEntity<UploadStatusDto> start(@RequestBody final UploadStartRequestDto request) {
        final var path = request.path();
        if (path == null || !DepotUtil.isValidPath(path)) {
            log.info("Invalid upload request {}", kv("path", path));
            throw new InvalidRequestException("path", String.valueOf(path), INVALID_REQUEST_DETAIL);
        }

        final var filename = request.filename();
        if (filename == null || !DepotUtil.isValidFilename(filename)) {
            log.info("Invalid upload request {}", kv("filename", filename));
            throw new InvalidRequestException("filename", String.valueOf(filename), INVALID_REQUEST_DETAIL);
        }

        if (request.size() <= 0) {
            log.info("Invalid upload request {}", kv("size", request.size()));
            throw new InvalidRequestException("size", String.valueOf(request.size()), "must be positive");
        }

        final var status = uploadService.start(path, filename, request.size(), request.hash());
        return ResponseEntity.created(URI.create("/upload/" + status.uploadId())).body(status);
    }

    @GetMapping("/upload/{id}")
    @PreAuthorize("hasRole('WRITE')")
    @Operation(summary = "Get the progress of an upload, e.g. to resume after an interruption")
    public ResponseEntity<UploadStatusDto> status(@PathVariable final String id) {
        return ResponseEntity.ok(uploadService.status(id));
    }

    @PutMapping(value = "/upload/{id}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasRole('WRITE')")
    @Operation(summary = "Append a chunk; the offset must equal the bytes received so far, otherwise 409 reports them")
    public ResponseEntity<UploadStatusDto> chunk(@PathVariable final String id,
                                                 @Parameter(description = "Byte offset of this chunk within the file", example = "8388608") @RequestParam final long offset,
                                                 @Parameter(description = "Optional hex SHA-256 of the chunk") @RequestHeader(value = "X-Chunk-Checksum", required = false) final String checksum,
                                                 final HttpServletRequest request) throws IOException {
        return ResponseEntity.ok(uploadService.append(id, offset, request.getInputStream(), checksum));
    }

    @PostMapping("/upload/{id}/complete")
    @PreAuthorize("hasRole('WRITE')")
    @Operation(summary = "Complete an upload once all bytes are received and store the file")
    public ResponseEntity<PutFileResponseDto> complete(@PathVariable final String id) {
        return ResponseEntity.ok(uploadService.complete(id));
    }

    @DeleteMapping("/upload/{id}")
    @PreAuthorize("hasRole('WRITE')")
    @Operation(summary = "Abort an upload and discard the received data")
    public ResponseEntity<Void> abort(@PathVariable final String id) {
        uploadService.abort(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
