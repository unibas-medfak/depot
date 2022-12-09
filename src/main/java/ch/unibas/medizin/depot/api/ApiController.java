package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.dto.FileDto;
import ch.unibas.medizin.depot.dto.PutFileResponseDto;
import ch.unibas.medizin.depot.service.DepotService;
import ch.unibas.medizin.depot.util.DepotUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "depotapi")
public class ApiController {

    private final DepotService depotService;

    @GetMapping("/list")
    @PreAuthorize("hasRole('READ')")
    @Operation(summary = "List all files and folders in the given path")
    public ResponseEntity<List<FileDto>> list(@Parameter(description = "Path to be listed") @RequestParam("path") final String path) {
        if (!DepotUtil.validPath(path)) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(depotService.list(path));
    }

    @GetMapping("/get")
    @PreAuthorize("hasRole('READ')")
    @Operation(summary = "Retrieve a file")
    public Resource get(@Parameter(description = "File to be retrieved") @RequestParam("file") final String file) {
        if (!DepotUtil.validAbsolutPath(file)) {
            log.error("Access denied for request of file {}", file);
            return null;
        }

        return depotService.get(file);
    }

    @PostMapping(value = "/put", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WRITE')")
    @Operation(summary = "Store a file")
    public ResponseEntity<PutFileResponseDto> put(@Parameter(description = "Multipart file to be stored") @RequestParam("file") final MultipartFile file,
                                                  @Parameter(description = "Path where the file will be stored") @RequestParam final String path,
                                                  @Parameter(description = "Whether the response shall contain a SHA256 hash of the stored data") @RequestParam(required = false) final boolean hash) {
        if (!DepotUtil.validPath(path)) {
            return ResponseEntity.badRequest().build();
        }

        if (!DepotUtil.validFilename(file.getName())) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(depotService.put(file, path, hash));
    }

}
