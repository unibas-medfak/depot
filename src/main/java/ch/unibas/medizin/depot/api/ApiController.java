package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.dto.FileDto;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import ch.unibas.medizin.depot.exception.InvlidRequestException;
import ch.unibas.medizin.depot.service.DepotService;
import ch.unibas.medizin.depot.util.DepotUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@SecurityRequirement(name = "depotapi")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private static final String INVALID_REQUEST_DETAIL = "must only contain letters, digits and the following chars . _ - @ % + /";

    private final DepotService depotService;

    public ApiController(DepotService depotService) {
        this.depotService = depotService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('READ')")
    @Operation(summary = "List all files and folders in the given path")
    public ResponseEntity<List<FileDto>> list(@Parameter(description = "Path to be listed", example = "pictures/cats") @RequestParam("path") final String path) {
        if (!DepotUtil.isValidPath(path)) {
            log.error("Invalid request - list path {}", path);
            throw new InvlidRequestException("path", path, INVALID_REQUEST_DETAIL);
        }

        return ResponseEntity.ok(depotService.list(path));
    }

    @GetMapping("/get")
    @PreAuthorize("hasRole('READ')")
    @Operation(summary = "Retrieve a file")
    public Resource get(@Parameter(description = "Filename and Path to be retrieved", example = "pictures/cats/cat.png") @RequestParam("file") final String file) {
        if (!DepotUtil.isValidAbsolutPath(file)) {
            log.error("Invalid request - get file {}", file);
            throw new InvlidRequestException("file", file, INVALID_REQUEST_DETAIL);
        }

        return depotService.get(file);
    }

    @PostMapping(value = "/put", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WRITE')")
    @Operation(summary = "Store a file")
    public ResponseEntity<PutFileResponseDto> put(@Parameter(description = "Multipart file to be stored") @RequestParam("file") final MultipartFile file,
                                                  @Parameter(description = "Path where the file will be stored", example = "pictures/cats") @RequestParam final String path,
                                                  @Parameter(description = "Whether the response shall contain a SHA256 hash of the stored data", example = "true") @RequestParam(required = false) final boolean hash) {

        if (!DepotUtil.isValidPath(path)) {
            log.error("Invalid request - put path {}", path);
            throw new InvlidRequestException("path", path, INVALID_REQUEST_DETAIL);
        }

        if (!DepotUtil.isValidFilename(file.getOriginalFilename())) {
            log.error("Invalid request - put filename {}", file.getOriginalFilename());
            throw new InvlidRequestException("filename", file.getOriginalFilename(), INVALID_REQUEST_DETAIL);
        }

        return ResponseEntity.ok(depotService.put(file, path, hash));
    }

    @GetMapping("/delete")
    @PreAuthorize("hasRole('DELETE')")
    @Operation(summary = "Delete a file or folder at the given path")
    public ResponseEntity<Void> delete(@Parameter(description = "Filename or Folder to be delete", example = "pictures/cats/cat.png") @RequestParam("path") final String path) {
        if (!DepotUtil.isValidPath(path)) {
            log.error("Invalid request - delete path {}", path);
            throw new InvlidRequestException("path", path, INVALID_REQUEST_DETAIL);
        }

        depotService.delete(path);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
